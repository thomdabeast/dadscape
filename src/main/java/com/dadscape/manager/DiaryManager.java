package com.dadscape.manager;

import com.dadscape.model.ClanDiary;
import com.dadscape.model.DiaryTask;
import com.dadscape.model.DiaryTier;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages CRUD operations for clan diaries.
 * Handles in-memory diary management and delegates storage to DiaryStorageService.
 * Syncs with backend API when enabled.
 */
@Slf4j
@Singleton
public class DiaryManager
{
	private final DiaryStorageService storageService;
	private final ApiService apiService;
	private final PermissionManager permissionManager;
	private List<ClanDiary> diaries;
	private String messageOfTheDay;

	@Inject
	public DiaryManager(
		DiaryStorageService storageService,
		ApiService apiService,
		PermissionManager permissionManager
	)
	{
		this.storageService = storageService;
		this.apiService = apiService;
		this.permissionManager = permissionManager;
		this.diaries = new ArrayList<>();
		this.messageOfTheDay = "";
	}

	/**
	 * Load all diaries from storage
	 */
	public void loadDiaries()
	{
		try
		{
			this.diaries = storageService.loadDiaries();
			this.messageOfTheDay = storageService.loadMotd();
			log.info("Loaded {} diaries", diaries.size());
		}
		catch (Exception e)
		{
			log.error("Failed to load diaries", e);
			this.diaries = new ArrayList<>();
			this.messageOfTheDay = "";
		}
	}

	/**
	 * Save all diaries to storage
	 */
	public void saveDiaries()
	{
		try
		{
			storageService.saveDiaries(diaries);
			log.info("Saved {} diaries", diaries.size());
		}
		catch (Exception e)
		{
			log.error("Failed to save diaries", e);
		}
	}

	/**
	 * Get all diaries
	 */
	public List<ClanDiary> getAllDiaries()
	{
		return new ArrayList<>(diaries);
	}

	/**
	 * Get diaries by category
	 */
	public List<ClanDiary> getDiariesByCategory(String category)
	{
		List<ClanDiary> result = new ArrayList<>();
		for (ClanDiary diary : diaries)
		{
			if (diary.getCategory().equalsIgnoreCase(category))
			{
				result.add(diary);
			}
		}
		return result;
	}

	/**
	 * Get active diaries only
	 */
	public List<ClanDiary> getActiveDiaries()
	{
		List<ClanDiary> result = new ArrayList<>();
		for (ClanDiary diary : diaries)
		{
			if (diary.isActive())
			{
				result.add(diary);
			}
		}
		return result;
	}

	/**
	 * Get diary by ID
	 */
	public ClanDiary getDiaryById(String id)
	{
		return diaries.stream()
			.filter(d -> d.getId().equals(id))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Sync diaries from API to local storage
	 */
	public void syncFromApi()
	{
		if (!apiService.isConfigured())
		{
			log.debug("API sync not configured, skipping");
			return;
		}

		apiService.fetchDiaries().thenAccept(remoteDiaries -> {
			if (remoteDiaries != null && !remoteDiaries.isEmpty())
			{
				this.diaries = remoteDiaries;
				saveDiaries(); // Cache locally
				log.info("Synced {} diaries from API", remoteDiaries.size());
			}
		}).exceptionally(ex -> {
			log.warn("Failed to sync diaries from API, using local cache", ex);
			return null;
		});

		// Also sync MOTD
		apiService.fetchMotd().thenAccept(remoteMot -> {
			if (remoteMot != null && !remoteMot.isEmpty())
			{
				this.messageOfTheDay = remoteMot;
				storageService.saveMotd(remoteMot);
				log.debug("Synced MOTD from API");
			}
		}).exceptionally(ex -> {
			log.warn("Failed to sync MOTD from API", ex);
			return null;
		});
	}

	/**
	 * Create a new diary
	 */
	public ClanDiary createDiary(String name, String category, String createdBy)
	{
		ClanDiary diary = ClanDiary.create(name, category, createdBy);
		diaries.add(diary);
		saveDiaries(); // Save locally first

		// Sync to API if enabled
		if (apiService.isConfigured())
		{
			String rsn = permissionManager.getPlayerName();
			apiService.createDiary(diary, rsn).exceptionally(ex -> {
				log.error("Failed to sync new diary to API: {}", diary.getName(), ex);
				return null;
			});
		}

		log.info("Created new diary: {} ({})", name, category);
		return diary;
	}

	/**
	 * Update an existing diary
	 */
	public boolean updateDiary(ClanDiary updatedDiary)
	{
		for (int i = 0; i < diaries.size(); i++)
		{
			if (diaries.get(i).getId().equals(updatedDiary.getId()))
			{
				updatedDiary.updateModified(updatedDiary.getLastModifiedBy());
				diaries.set(i, updatedDiary);
				saveDiaries(); // Save locally first

				// Sync to API if enabled
				if (apiService.isConfigured())
				{
					String rsn = permissionManager.getPlayerName();
					apiService.updateDiary(updatedDiary, rsn).exceptionally(ex -> {
						log.error("Failed to sync diary update to API: {}", updatedDiary.getName(), ex);
						return null;
					});
				}

				log.info("Updated diary: {}", updatedDiary.getName());
				return true;
			}
		}
		return false;
	}

	/**
	 * Delete a diary by ID
	 */
	public boolean deleteDiary(String diaryId)
	{
		boolean removed = diaries.removeIf(d -> d.getId().equals(diaryId));
		if (removed)
		{
			saveDiaries(); // Save locally first

			// Sync to API if enabled
			if (apiService.isConfigured())
			{
				String rsn = permissionManager.getPlayerName();
				apiService.deleteDiary(diaryId, rsn).exceptionally(ex -> {
					log.error("Failed to sync diary deletion to API: {}", diaryId, ex);
					return false;
				});
			}

			log.info("Deleted diary: {}", diaryId);
		}
		return removed;
	}

	/**
	 * Add a tier to a diary
	 */
	public boolean addTier(String diaryId, DiaryTier tier)
	{
		ClanDiary diary = getDiaryById(diaryId);
		if (diary != null)
		{
			diary.addTier(tier);
			saveDiaries();
			return true;
		}
		return false;
	}

	/**
	 * Remove a tier from a diary
	 */
	public boolean removeTier(String diaryId, String tierName)
	{
		ClanDiary diary = getDiaryById(diaryId);
		if (diary != null)
		{
			boolean removed = diary.removeTier(tierName);
			if (removed)
			{
				saveDiaries();
			}
			return removed;
		}
		return false;
	}

	/**
	 * Add a task to a tier in a diary
	 */
	public boolean addTask(String diaryId, String tierName, DiaryTask task)
	{
		ClanDiary diary = getDiaryById(diaryId);
		if (diary != null)
		{
			DiaryTier tier = diary.getTierByName(tierName);
			if (tier != null)
			{
				tier.addTask(task);
				diary.updateModified(diary.getLastModifiedBy());
				saveDiaries();
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove a task from a diary
	 */
	public boolean removeTask(String diaryId, String tierName, String taskId)
	{
		ClanDiary diary = getDiaryById(diaryId);
		if (diary != null)
		{
			DiaryTier tier = diary.getTierByName(tierName);
			if (tier != null)
			{
				boolean removed = tier.removeTask(taskId);
				if (removed)
				{
					diary.updateModified(diary.getLastModifiedBy());
					saveDiaries();
				}
				return removed;
			}
		}
		return false;
	}

	/**
	 * Get all unique categories
	 */
	public List<String> getAllCategories()
	{
		List<String> categories = new ArrayList<>();
		for (ClanDiary diary : diaries)
		{
			if (!categories.contains(diary.getCategory()))
			{
				categories.add(diary.getCategory());
			}
		}
		categories.sort(String::compareToIgnoreCase);
		return categories;
	}

	/**
	 * Import a diary (replaces if exists, adds if new)
	 */
	public boolean importDiary(ClanDiary importedDiary)
	{
		// Check if diary with same ID already exists
		for (int i = 0; i < diaries.size(); i++)
		{
			if (diaries.get(i).getId().equals(importedDiary.getId()))
			{
				// Replace existing diary
				diaries.set(i, importedDiary);
				saveDiaries();
				log.info("Replaced existing diary: {}", importedDiary.getName());
				return true;
			}
		}

		// Add as new diary
		diaries.add(importedDiary);
		saveDiaries();
		log.info("Imported new diary: {}", importedDiary.getName());
		return true;
	}

	/**
	 * Get the current message of the day
	 */
	public String getMessageOfTheDay()
	{
		return messageOfTheDay != null ? messageOfTheDay : "";
	}

	/**
	 * Set the message of the day (admin only)
	 */
	public void setMessageOfTheDay(String message)
	{
		this.messageOfTheDay = message != null ? message : "";
		storageService.saveMotd(this.messageOfTheDay); // Save locally first

		// Sync to API if enabled
		if (apiService.isConfigured())
		{
			String rsn = permissionManager.getPlayerName();
			apiService.updateMotd(this.messageOfTheDay, rsn).exceptionally(ex -> {
				log.error("Failed to sync MOTD to API", ex);
				return false;
			});
		}

		log.info("Updated message of the day");
	}
}
