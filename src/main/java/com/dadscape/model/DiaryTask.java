package com.dadscape.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single achievement task within a diary tier.
 * Tasks can have flexible requirements stored as key-value pairs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiaryTask
{
	/**
	 * Unique identifier for this task
	 */
	private String id;

	/**
	 * Human-readable description of the task
	 * Example: "Kill 50 goblins in Lumbridge"
	 */
	private String description;

	/**
	 * The category/type of this task
	 */
	private TaskType type;

	/**
	 * Flexible key-value pairs for task requirements
	 * Examples:
	 * - KILL task: {"monster": "Goblin", "count": "50", "location": "Lumbridge"}
	 * - SKILL task: {"skill": "Woodcutting", "level": "50"}
	 * - QUEST task: {"quest": "Dragon Slayer II"}
	 */
	private Map<String, String> requirements;

	/**
	 * Optional hint or help text for completing the task
	 */
	private String hint;

	/**
	 * Display order within the tier (for sorting)
	 */
	private int order;

	/**
	 * Create a new task with generated UUID
	 */
	public static DiaryTask create(String description, TaskType type)
	{
		DiaryTask task = new DiaryTask();
		task.setId(UUID.randomUUID().toString());
		task.setDescription(description);
		task.setType(type);
		task.setRequirements(new HashMap<>());
		task.setHint("");
		task.setOrder(0);
		return task;
	}

	/**
	 * Add a requirement to this task
	 */
	public void addRequirement(String key, String value)
	{
		if (requirements == null)
		{
			requirements = new HashMap<>();
		}
		requirements.put(key, value);
	}

	/**
	 * Get a requirement value by key
	 */
	public String getRequirement(String key)
	{
		return requirements != null ? requirements.get(key) : null;
	}
}
