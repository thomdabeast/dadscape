package com.dadscape.manager;

import com.dadscape.DadScapeConfig;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages permission checking for diary editing based on clan rank.
 */
@Singleton
public class PermissionManager
{
	private final Client client;
	private final ClientThread clientThread;
	private int minEditRank;

	@Inject
	public PermissionManager(Client client, ClientThread clientThread, DadScapeConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.minEditRank = config.minEditRank();
	}

	/**
	 * Set the minimum rank required to edit diaries
	 */
	public void setMinEditRank(int rank)
	{
		this.minEditRank = rank;
	}

	/**
	 * Get the minimum rank required to edit diaries
	 */
	public int getMinEditRank()
	{
		return minEditRank;
	}

	/**
	 * Get the current player's clan member object
	 */
	private ClanChannelMember getCurrentClanMember()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		String playerName = player.getName();
		if (playerName == null)
		{
			return null;
		}

		System.out.println("Player name:" + playerName);

		ClanChannel clanChannel = client.getClanChannel();
		if (clanChannel == null)
		{
			return null;
		}

		System.out.println("Clan: " + clanChannel.getName());

		return clanChannel.findMember(playerName);
	}

	/**
	 * Check if the current player can edit diaries
	 */
	public boolean canEditDiary()
	{
		ClanChannelMember member = getCurrentClanMember();
		if (member == null)
		{
			return false;
		}

		ClanRank rank = member.getRank();
		if (rank == null)
		{
			return false;
		}

		// Compare rank value (higher values = higher rank)
		return rank.getRank() >= minEditRank;
	}

	/**
	 * Check if the current player is a clan leader (Owner)
	 */
	public boolean isClanLeader()
	{
		ClanChannelMember member = getCurrentClanMember();
		if (member == null)
		{
			return false;
		}

		ClanRank rank = member.getRank();
		return rank == ClanRank.OWNER;
	}

	/**
	 * Get the current player's clan rank value
	 * Returns -1 if player is null or not in a clan
	 */
	public int getCurrentRank()
	{
		ClanChannelMember member = getCurrentClanMember();
		if (member == null)
		{
			return -1;
		}

		ClanRank rank = member.getRank();
		if (rank == null)
		{
			return -1;
		}

		return rank.getRank();
	}

	/**
	 * Get the current player's clan rank as a display string
	 */
	public String getCurrentRankName()
	{
		// Execute on client thread to safely access clan settings
		String[] result = new String[1];
		result[0] = "Unknown";

		clientThread.invoke(() -> {
			ClanChannelMember member = getCurrentClanMember();
			if (member == null)
			{
				result[0] = "Not in clan";
				return;
			}

			ClanRank rank = member.getRank();
			if (rank == null)
			{
				result[0] = "Unknown";
				return;
			}

			// Use the clan title for display - with null safety
			ClanSettings clanSettings = client.getClanSettings();
			if (clanSettings == null)
			{
				result[0] = "Rank " + rank.getRank();
				return;
			}

			ClanTitle title = clanSettings.titleForRank(rank);
			if (title == null)
			{
				result[0] = "Rank " + rank.getRank();
				return;
			}

			result[0] = title.getName();
		});

		return result[0];
	}

	/**
	 * Get the current player's display name
	 */
	public String getPlayerName()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return "Unknown";
		}

		String name = player.getName();
		return name != null ? name : "Unknown";
	}

	/**
	 * Check if player is in a clan
	 */
	public boolean isInClan()
	{
		return getCurrentRank() >= 0;
	}
}
