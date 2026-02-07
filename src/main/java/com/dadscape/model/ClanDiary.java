package com.dadscape.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a complete clan achievement diary with metadata and tiers.
 * Supports multiple diaries per clan with categories.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClanDiary
{
	/**
	 * Unique identifier for this diary (UUID)
	 */
	private String id;

	/**
	 * Display name of the diary
	 * Example: "DadScape Achievement Diary"
	 */
	private String name;

	/**
	 * Brief description of the diary
	 */
	private String description;

	/**
	 * Category for organization (e.g., "PvM", "Skilling", "Collection Log")
	 */
	private String category;

	/**
	 * Version string for tracking updates
	 * Example: "1.0", "1.1", "2.0"
	 */
	private String version;

	/**
	 * Timestamp when diary was created (Unix epoch milliseconds)
	 */
	private long createdDate;

	/**
	 * RuneScape name of the player who created this diary
	 */
	private String createdBy;

	/**
	 * Timestamp when diary was last modified
	 */
	private long lastModified;

	/**
	 * RuneScape name of the player who last modified this diary
	 */
	private String lastModifiedBy;

	/**
	 * List of tiers (Easy, Medium, Hard, Elite, etc.)
	 */
	private List<DiaryTier> tiers;

	/**
	 * Whether this diary is currently active/published
	 */
	private boolean active;

	/**
	 * Create a new diary with generated UUID and current timestamp
	 */
	public static ClanDiary create(String name, String category, String createdBy)
	{
		ClanDiary diary = new ClanDiary();
		diary.setId(UUID.randomUUID().toString());
		diary.setName(name);
		diary.setDescription("");
		diary.setCategory(category);
		diary.setVersion("1.0");
		diary.setCreatedDate(System.currentTimeMillis());
		diary.setCreatedBy(createdBy);
		diary.setLastModified(System.currentTimeMillis());
		diary.setLastModifiedBy(createdBy);
		diary.setTiers(new ArrayList<>());
		diary.setActive(true);
		return diary;
	}

	/**
	 * Add a tier to this diary
	 */
	public void addTier(DiaryTier tier)
	{
		if (tiers == null)
		{
			tiers = new ArrayList<>();
		}
		tier.setOrder(tiers.size());
		tiers.add(tier);
		updateModified(lastModifiedBy);
	}

	/**
	 * Remove a tier by name
	 */
	public boolean removeTier(String tierName)
	{
		if (tiers == null)
		{
			return false;
		}
		boolean removed = tiers.removeIf(tier -> tier.getTierName().equals(tierName));
		if (removed)
		{
			updateModified(lastModifiedBy);
		}
		return removed;
	}

	/**
	 * Get tier by name
	 */
	public DiaryTier getTierByName(String tierName)
	{
		if (tiers == null)
		{
			return null;
		}
		return tiers.stream()
			.filter(tier -> tier.getTierName().equals(tierName))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Get total count of all tasks across all tiers
	 */
	public int getTotalTaskCount()
	{
		if (tiers == null)
		{
			return 0;
		}
		return tiers.stream()
			.mapToInt(DiaryTier::getTaskCount)
			.sum();
	}

	/**
	 * Update the last modified timestamp and user
	 */
	public void updateModified(String modifiedBy)
	{
		this.lastModified = System.currentTimeMillis();
		this.lastModifiedBy = modifiedBy;
	}

	/**
	 * Increment version (e.g., "1.0" -> "1.1")
	 */
	public void incrementVersion()
	{
		try
		{
			String[] parts = version.split("\\.");
			if (parts.length == 2)
			{
				int minor = Integer.parseInt(parts[1]) + 1;
				this.version = parts[0] + "." + minor;
			}
		}
		catch (Exception e)
		{
			// If version parsing fails, just append .1
			this.version = this.version + ".1";
		}
	}
}
