package com.dadscape.manager;

import com.dadscape.model.DadCred;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages DadCred data for the current player.
 * Can fetch from local storage or external API.
 */
@Slf4j
@Singleton
public class DadCredManager
{
	private static final String CONFIG_GROUP = "dadscape";
	private static final String DADCRED_KEY = "dadcred";

	private final ConfigManager configManager;
	private final Gson gson;
	private DadCred currentDadCred;

	@Inject
	public DadCredManager(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	/**
	 * Load DadCred from local storage
	 */
	public DadCred loadDadCred(String rsn)
	{
		try
		{
			String json = configManager.getConfiguration(CONFIG_GROUP, DADCRED_KEY);
			if (json != null && !json.isEmpty())
			{
				currentDadCred = gson.fromJson(json, DadCred.class);
				log.debug("Loaded DadCred for {}: {}", rsn, currentDadCred.getDisplayString());
			}
			else
			{
				// No saved cred, create default
				currentDadCred = DadCred.createUnranked(rsn);
				saveDadCred(currentDadCred);
				log.debug("Created new DadCred for {}", rsn);
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load DadCred, creating default", e);
			currentDadCred = DadCred.createUnranked(rsn);
		}

		return currentDadCred;
	}

	/**
	 * Save DadCred to local storage
	 */
	public void saveDadCred(DadCred dadCred)
	{
		try
		{
			String json = gson.toJson(dadCred);
			configManager.setConfiguration(CONFIG_GROUP, DADCRED_KEY, json);
			this.currentDadCred = dadCred;
			log.debug("Saved DadCred: {}", dadCred.getDisplayString());
		}
		catch (Exception e)
		{
			log.error("Failed to save DadCred", e);
		}
	}

	/**
	 * Get the current player's DadCred
	 */
	public DadCred getCurrentDadCred()
	{
		return currentDadCred;
	}

	/**
	 * Update the cred score
	 */
	public void updateCredScore(int newScore)
	{
		if (currentDadCred != null)
		{
			currentDadCred.setCredScore(newScore);
			currentDadCred.setLastUpdated(System.currentTimeMillis());
			saveDadCred(currentDadCred);
		}
	}

	/**
	 * Fetch DadCred from external API (future implementation)
	 */
	public void fetchFromApi(String rsn, String apiEndpoint)
	{
		// TODO: Implement API fetch
		log.debug("API fetch not yet implemented");
	}

	/**
	 * Sync DadCred to external API (future implementation)
	 */
	public void syncToApi(DadCred dadCred, String apiEndpoint)
	{
		// TODO: Implement API sync
		log.debug("API sync not yet implemented");
	}
}
