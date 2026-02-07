package com.dadscape;

import com.dadscape.manager.DadCredManager;
import com.dadscape.manager.DiaryManager;
import com.dadscape.manager.PermissionManager;
import com.dadscape.manager.TaskTracker;
import com.dadscape.ui.DadScapePanel;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "DadScape"
)
public class DadScapePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DadScapeConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private DiaryManager diaryManager;

	@Inject
	private PermissionManager permissionManager;

	@Inject
	private DadCredManager dadCredManager;

	@Inject
	private TaskTracker taskTracker;

	@Inject
	private DadScapePanel panel;

	@Inject
	private ConfigManager configManager;

	private NavigationButton navButton;
	private boolean initialRefreshDone = false;

	@Override
	protected void startUp() throws Exception
	{
		log.info("DadScape started!");

		// One-time migration: Reset minEditRank if it's still set to old default (125)
		// Remove this code block after users have migrated
		if (config.minEditRank() == 125)
		{
			log.info("Migrating minEditRank from old default (125) to new default (0)");
			configManager.unsetConfiguration("dadscape", "minEditRank");
		}

		// Initialize permission manager with config
		permissionManager.setMinEditRank(config.minEditRank());
		log.debug("Set minimum edit rank to: {}", config.minEditRank());

		// Load diaries from storage
		diaryManager.loadDiaries();

		// Sync from API if configured
		if (config.enableApiSync() && config.apiKey() != null && !config.apiKey().isEmpty())
		{
			log.info("API sync enabled, fetching diaries from API");
			diaryManager.syncFromApi();
		}

		// Add panel to sidebar
		addPanel();

		// Reset refresh flag
		initialRefreshDone = false;
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("DadScape stopped!");

		// Clear task tracker cache
		taskTracker.clearCache();

		// Remove panel from sidebar
		removePanel();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			// Update permission manager with config
			permissionManager.setMinEditRank(config.minEditRank());
			log.debug("Updated minimum edit rank on login: {}", config.minEditRank());

			// Reset refresh flag so we refresh after clan data loads
			initialRefreshDone = false;

			// Display message of the day if it exists
			String motd = diaryManager.getMessageOfTheDay();
			if (motd != null && !motd.trim().isEmpty())
			{
				// Show MOTD in chat with a slight delay to ensure game is ready
				clientThread.invokeLater(() -> {
					client.addChatMessage(
						ChatMessageType.CLAN_CHAT,
						"DadBot",
						"<col=00ff00>Message of the Day:</col> " + motd,
						"DadScape"
					);
				});
			}
		}
	}

	/**
	 * Listen for clan channel changes (when you join/leave a clan)
	 */
	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		log.debug("Clan channel changed, refreshing admin panel");

		// Refresh panel when clan data changes
		if (panel != null)
		{
			panel.refreshDadCred();
			panel.refreshPermissionStatus();
			panel.refreshDiaryList();
		}
	}

	/**
	 * Use game tick to do an initial refresh after login
	 * This ensures clan data is loaded before we try to read it
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Only do this once after login
		if (!initialRefreshDone && client.getGameState() == GameState.LOGGED_IN)
		{
			// Check if we have clan data
			if (client.getClanChannel() != null || client.getLocalPlayer() != null)
			{
				log.debug("Initial refresh of panel with clan data");

				if (panel != null)
				{
					panel.refreshDadCred();
					panel.refreshPermissionStatus();
					panel.refreshDiaryList();
				}

				initialRefreshDone = true;
			}
		}
	}

	/**
	 * Listen for config changes to update plugin in real-time
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		// Only respond to DadScape config changes
		if (!event.getGroup().equals("dadscape"))
		{
			return;
		}

		log.debug("Config changed: {} = {}", event.getKey(), event.getNewValue());

		// Handle specific config changes
		switch (event.getKey())
		{
			case "minEditRank":
				// Update permission manager with new minimum rank
				permissionManager.setMinEditRank(config.minEditRank());
				log.debug("Updated minimum edit rank to: {}", config.minEditRank());

				// Refresh panel to update permission status
				if (panel != null)
				{
					panel.refreshPermissionStatus();
				}
				break;

			case "enableDiaries":
				// Reload diaries if feature was toggled
				if (config.enableDiaries())
				{
					diaryManager.loadDiaries();
					if (panel != null)
					{
						panel.refreshDiaryList();
					}
				}
				break;

			case "enableApiSync":
			case "apiEndpoint":
			case "apiKey":
				// Re-sync from API when API settings change
				if (config.enableApiSync() && config.apiKey() != null && !config.apiKey().isEmpty())
				{
					log.info("API settings changed, re-syncing from API");
					diaryManager.syncFromApi();
					if (panel != null)
					{
						panel.refreshDiaryList();
					}
				}
				break;
		}
	}

	/**
	 * Listen for chat messages to track consumable/custom tasks
	 */
	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		// Only track SPAM messages (item consumption, etc.)
		if (chatMessage.getType() != ChatMessageType.SPAM)
		{
			return;
		}

		String message = chatMessage.getMessage();
		if (message == null || message.isEmpty())
		{
			return;
		}

		// Get player name
		String playerName = permissionManager.getPlayerName();
		if (playerName == null || playerName.equals("Unknown"))
		{
			return;
		}

		// Track the chat message for custom tasks
		log.debug("Chat SPAM message: {}", message);
		taskTracker.onChatMessage(playerName, message);
	}

	/**
	 * Listen for NPC deaths to track kill tasks
	 */
	@Subscribe
	public void onActorDeath(ActorDeath actorDeath)
	{
		// Only track NPC deaths
		if (!(actorDeath.getActor() instanceof NPC))
		{
			return;
		}

		NPC npc = (NPC) actorDeath.getActor();
		String npcName = npc.getName();

		if (npcName == null)
		{
			return;
		}

		// Check if there was combat interaction between the player and NPC
		// This ensures we only track kills by the local player
		// Check both directions to handle one-shots and normal combat
		boolean npcWasAttackingPlayer = npc.getInteracting() == client.getLocalPlayer();
		boolean playerWasAttackingNpc = client.getLocalPlayer().getInteracting() == npc;

		if (!npcWasAttackingPlayer && !playerWasAttackingNpc)
		{
			log.debug("NPC {} died but had no combat interaction with local player, skipping", npcName);
			return;
		}

		// Get player name
		String playerName = permissionManager.getPlayerName();
		if (playerName == null || playerName.equals("Unknown"))
		{
			return;
		}

		// Track the kill
		log.debug("NPC killed by player: {}", npcName);
		taskTracker.onNpcKilled(playerName, npcName);
	}

	/**
	 * Add the DadScape panel to the client toolbar
	 */
	private void addPanel()
	{
		if (navButton != null)
		{
			return; // Already added
		}

		// Try to load icon, use null if not found (RuneLite will use default)
		BufferedImage icon = null;
		try
		{
			icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		}
		catch (Exception e)
		{
			log.debug("No custom icon found, using default");
		}

		navButton = NavigationButton.builder()
			.tooltip("DadScape")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.debug("Added DadScape panel to toolbar");
	}

	/**
	 * Remove the DadScape panel from the client toolbar
	 */
	private void removePanel()
	{
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
			navButton = null;
			log.debug("Removed DadScape panel from toolbar");
		}
	}

	@Provides
	DadScapeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DadScapeConfig.class);
	}
}

