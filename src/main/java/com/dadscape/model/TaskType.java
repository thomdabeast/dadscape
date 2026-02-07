package com.dadscape.model;

/**
 * Represents the type/category of an achievement task.
 * Used to classify tasks and potentially enable auto-detection of completion.
 */
public enum TaskType
{
	/**
	 * Tasks involving killing monsters or NPCs
	 * Example: "Kill 50 goblins in Lumbridge"
	 */
	KILL("Kill/Combat", "#FF4444"),

	/**
	 * Tasks involving skilling activities or level requirements
	 * Example: "Reach 50 Woodcutting" or "Cut 100 magic logs"
	 */
	SKILL("Skilling", "#44FF44"),

	/**
	 * Tasks involving quest completion
	 * Example: "Complete Dragon Slayer II"
	 */
	QUEST("Quest", "#4444FF"),

	/**
	 * Tasks involving obtaining or equipping items
	 * Example: "Obtain a Dragon Warhammer" or "Equip full Bandos"
	 */
	ITEM("Item/Equipment", "#FFAA44"),

	/**
	 * Tasks involving visiting specific locations
	 * Example: "Visit the Myths' Guild"
	 */
	LOCATION("Location", "#AA44FF"),

	/**
	 * Tasks involving PvM boss kills
	 * Example: "Defeat Zulrah 10 times"
	 */
	BOSS("Boss/PvM", "#FF44AA"),

	/**
	 * Tasks involving minigames
	 * Example: "Complete 5 games of Barbarian Assault"
	 */
	MINIGAME("Minigame", "#44FFFF"),

	/**
	 * Custom/freeform tasks that don't fit other categories
	 * Example: "Take a screenshot at the Grand Exchange"
	 */
	CUSTOM("Custom", "#AAAAAA");

	private final String displayName;
	private final String colorHex;

	TaskType(String displayName, String colorHex)
	{
		this.displayName = displayName;
		this.colorHex = colorHex;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getColorHex()
	{
		return colorHex;
	}
}
