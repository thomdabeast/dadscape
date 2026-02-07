package com.dadscape.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks a user's progress on diary tasks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress
{
	/**
	 * User's RSN
	 */
	private String rsn;

	/**
	 * Diary ID
	 */
	private String diaryId;

	/**
	 * Map of task ID to progress count
	 * For KILL tasks, this is the number of kills
	 * For SKILL tasks, this could be the level achieved
	 */
	private Map<String, Integer> taskProgress;

	/**
	 * Map of task ID to completion status
	 */
	private Map<String, Boolean> taskCompletion;

	/**
	 * Timestamp when progress was last updated
	 */
	private long lastUpdated;

	/**
	 * Create new progress tracker for a user and diary
	 */
	public static UserProgress create(String rsn, String diaryId)
	{
		UserProgress progress = new UserProgress();
		progress.setRsn(rsn);
		progress.setDiaryId(diaryId);
		progress.setTaskProgress(new HashMap<>());
		progress.setTaskCompletion(new HashMap<>());
		progress.setLastUpdated(System.currentTimeMillis());
		return progress;
	}

	/**
	 * Get progress for a specific task
	 */
	public int getTaskProgress(String taskId)
	{
		return taskProgress.getOrDefault(taskId, 0);
	}

	/**
	 * Update progress for a task
	 */
	public void updateTaskProgress(String taskId, int progress)
	{
		taskProgress.put(taskId, progress);
		lastUpdated = System.currentTimeMillis();
	}

	/**
	 * Increment progress for a task
	 */
	public void incrementTaskProgress(String taskId, int amount)
	{
		int current = getTaskProgress(taskId);
		updateTaskProgress(taskId, current + amount);
	}

	/**
	 * Check if a task is completed
	 */
	public boolean isTaskCompleted(String taskId)
	{
		return taskCompletion.getOrDefault(taskId, false);
	}

	/**
	 * Mark a task as completed
	 */
	public void completeTask(String taskId)
	{
		taskCompletion.put(taskId, true);
		lastUpdated = System.currentTimeMillis();
	}

	/**
	 * Get total completion percentage for this diary
	 */
	public int getCompletionPercentage(int totalTasks)
	{
		if (totalTasks == 0)
		{
			return 0;
		}

		long completedCount = taskCompletion.values().stream()
			.filter(completed -> completed)
			.count();

		return (int) ((completedCount * 100) / totalTasks);
	}
}
