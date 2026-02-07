package com.dadscape.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a difficulty tier within an achievement diary.
 * Similar to OSRS diary tiers (Easy, Medium, Hard, Elite).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTier
{
	/**
	 * Name of the tier (e.g., "Easy", "Medium", "Hard", "Elite")
	 */
	private String tierName;

	/**
	 * Color hex code for UI display (e.g., "#00FF00" for green)
	 */
	private String tierColor;

	/**
	 * List of tasks in this tier
	 */
	private List<DiaryTask> tasks;

	/**
	 * Optional description of rewards for completing this tier
	 */
	private String rewardDescription;

	/**
	 * Display order for this tier (0 = first, higher = later)
	 */
	private int order;

	/**
	 * Create a new tier with the given name
	 */
	public static DiaryTier create(String tierName, String tierColor, int order)
	{
		DiaryTier tier = new DiaryTier();
		tier.setTierName(tierName);
		tier.setTierColor(tierColor);
		tier.setTasks(new ArrayList<>());
		tier.setRewardDescription("");
		tier.setOrder(order);
		return tier;
	}

	/**
	 * Add a task to this tier
	 */
	public void addTask(DiaryTask task)
	{
		if (tasks == null)
		{
			tasks = new ArrayList<>();
		}
		task.setOrder(tasks.size());
		tasks.add(task);
	}

	/**
	 * Remove a task by ID
	 */
	public boolean removeTask(String taskId)
	{
		if (tasks == null)
		{
			return false;
		}
		return tasks.removeIf(task -> task.getId().equals(taskId));
	}

	/**
	 * Get total number of tasks in this tier
	 */
	public int getTaskCount()
	{
		return tasks != null ? tasks.size() : 0;
	}

	/**
	 * Get task by ID
	 */
	public DiaryTask getTaskById(String taskId)
	{
		if (tasks == null)
		{
			return null;
		}
		return tasks.stream()
			.filter(task -> task.getId().equals(taskId))
			.findFirst()
			.orElse(null);
	}
}
