package com.dadscape.manager;

import com.dadscape.model.ClanDiary;
import com.dadscape.model.DiaryTask;
import com.dadscape.model.DiaryTier;
import com.dadscape.model.TaskType;
import com.dadscape.model.UserProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks user progress on diary tasks and handles auto-completion
 */
@Slf4j
@Singleton
public class TaskTracker
{
	private static final String CONFIG_GROUP = "dadscape";
	private static final String PROGRESS_KEY_PREFIX = "progress_";

	private final ConfigManager configManager;
	private final DiaryManager diaryManager;
	private final Client client;
	private final Gson gson;

	// In-memory cache of user progress
	private final Map<String, UserProgress> progressCache;

	@Inject
	public TaskTracker(ConfigManager configManager, DiaryManager diaryManager, Client client, Gson gson)
	{
		this.configManager = configManager;
		this.diaryManager = diaryManager;
		this.client = client;
		this.gson = gson;
		this.progressCache = new HashMap<>();
	}

	/**
	 * Load progress for a user and diary
	 */
	public UserProgress loadProgress(String rsn, String diaryId)
	{
		String key = PROGRESS_KEY_PREFIX + diaryId;
		String cacheKey = rsn + "_" + diaryId;

		// Check cache first
		if (progressCache.containsKey(cacheKey))
		{
			return progressCache.get(cacheKey);
		}

		// Load from config
		try
		{
			String json = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
			if (json != null && !json.isEmpty())
			{
				Type mapType = new TypeToken<Map<String, UserProgress>>(){}.getType();
				Map<String, UserProgress> allProgress = gson.fromJson(json, mapType);

				if (allProgress != null && allProgress.containsKey(rsn))
				{
					UserProgress progress = allProgress.get(rsn);
					progressCache.put(cacheKey, progress);
					return progress;
				}
			}
		}
		catch (Exception e)
		{
			log.error("Failed to load progress for {} on diary {}", rsn, diaryId, e);
		}

		// Create new progress
		UserProgress progress = UserProgress.create(rsn, diaryId);
		progressCache.put(cacheKey, progress);
		return progress;
	}

	/**
	 * Save progress for a user
	 */
	public void saveProgress(UserProgress progress)
	{
		String key = PROGRESS_KEY_PREFIX + progress.getDiaryId();
		String cacheKey = progress.getRsn() + "_" + progress.getDiaryId();

		try
		{
			// Load all progress for this diary
			Map<String, UserProgress> allProgress = new HashMap<>();

			String json = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
			if (json != null && !json.isEmpty())
			{
				Type mapType = new TypeToken<Map<String, UserProgress>>(){}.getType();
				Map<String, UserProgress> loaded = gson.fromJson(json, mapType);
				if (loaded != null)
				{
					allProgress = loaded;
				}
			}

			// Update this user's progress
			allProgress.put(progress.getRsn(), progress);

			// Save back to config
			String updatedJson = gson.toJson(allProgress);
			configManager.setRSProfileConfiguration(CONFIG_GROUP, key, updatedJson);

			// Update cache
			progressCache.put(cacheKey, progress);

			log.debug("Saved progress for {} on diary {}", progress.getRsn(), progress.getDiaryId());
		}
		catch (Exception e)
		{
			log.error("Failed to save progress", e);
		}
	}

	/**
	 * Handle chat message event for tracking custom/consumable tasks
	 */
	public void onChatMessage(String rsn, String chatMessage)
	{
		// Check all active diaries for CUSTOM tasks with chat patterns
		for (ClanDiary diary : diaryManager.getActiveDiaries())
		{
			UserProgress progress = loadProgress(rsn, diary.getId());

			for (DiaryTier tier : diary.getTiers())
			{
				for (DiaryTask task : tier.getTasks())
				{
					// Check for CUSTOM tasks with chat pattern tracking
					if (task.getType() == TaskType.CUSTOM)
					{
						String chatPattern = task.getRequirement("chatPattern");
						String requiredCount = task.getRequirement("count");

						if (chatPattern != null && requiredCount != null)
						{
							// Check if chat message contains the pattern (case-insensitive)
							if (chatMessage.toLowerCase().contains(chatPattern.toLowerCase()))
							{
								// Increment progress count
								progress.incrementTaskProgress(task.getId(), 1);

								int currentCount = progress.getTaskProgress(task.getId());
								int targetCount = Integer.parseInt(requiredCount);

								log.debug("Custom task progress for {}: {}/{} ({})", rsn, currentCount, targetCount, task.getDescription());

								// Check if task is completed
								if (currentCount >= targetCount && !progress.isTaskCompleted(task.getId()))
								{
									progress.completeTask(task.getId());
									log.info("Task completed: {} - {}", rsn, task.getDescription());

									// Show in-game notification
									showCompletionMessage(task.getDescription(), tier.getTierName());
								}

								saveProgress(progress);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Handle NPC kill event
	 */
	public void onNpcKilled(String rsn, String npcName)
	{
		// Check all active diaries for KILL tasks matching this NPC
		for (ClanDiary diary : diaryManager.getActiveDiaries())
		{
			UserProgress progress = loadProgress(rsn, diary.getId());

			for (DiaryTier tier : diary.getTiers())
			{
				for (DiaryTask task : tier.getTasks())
				{
					if (task.getType() == TaskType.KILL)
					{
						String requiredNpc = task.getRequirement("npc");
						String requiredCount = task.getRequirement("count");

						if (requiredNpc != null && requiredCount != null)
						{
							// Check if NPC name matches (case-insensitive)
							if (npcName.equalsIgnoreCase(requiredNpc))
							{
								// Increment kill count
								progress.incrementTaskProgress(task.getId(), 1);

								int currentKills = progress.getTaskProgress(task.getId());
								int targetKills = Integer.parseInt(requiredCount);

								log.debug("Kill progress for {}: {}/{} {}", rsn, currentKills, targetKills, npcName);

								// Check if task is completed
								if (currentKills >= targetKills && !progress.isTaskCompleted(task.getId()))
								{
									progress.completeTask(task.getId());
									log.info("Task completed: {} - {}", rsn, task.getDescription());

									// Show in-game notification
									showCompletionMessage(task.getDescription(), tier.getTierName());
								}

								saveProgress(progress);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Get completion percentage for a diary
	 */
	public int getDiaryCompletion(String rsn, String diaryId)
	{
		ClanDiary diary = diaryManager.getDiaryById(diaryId);
		if (diary == null)
		{
			return 0;
		}

		UserProgress progress = loadProgress(rsn, diaryId);
		return progress.getCompletionPercentage(diary.getTotalTaskCount());
	}

	/**
	 * Show in-game notification for task completion
	 */
	private void showCompletionMessage(String taskDescription, String tierName)
	{
		String message = "<col=00ff00>Diary Task Complete!</col> [" + tierName + "] " + taskDescription;
		client.addChatMessage(
			ChatMessageType.GAMEMESSAGE,
			"",
			message,
			null
		);
	}

	/**
	 * Clear progress cache (call when logging out)
	 */
	public void clearCache()
	{
		progressCache.clear();
	}
}
