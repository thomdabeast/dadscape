package com.dadscape;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("dadscape")
public interface DadScapeConfig extends Config
{
	@ConfigSection(
		name = "Clan Diary",
		description = "Clan achievement diary settings",
		position = 1
	)
	String diarySection = "diary";

	@ConfigItem(
		keyName = "enableDiaries",
		name = "Enable Clan Diaries",
		description = "Enable the clan achievement diary feature",
		section = diarySection,
		position = 0
	)
	default boolean enableDiaries()
	{
		return true;
	}

	@ConfigItem(
		keyName = "minEditRank",
		name = "Minimum Edit Rank",
		description = "Minimum clan rank required to create/edit diaries (125 = Captain, 127 = Owner)",
		section = diarySection,
		position = 1
	)
	@Range(min = 0, max = 127)
	default int minEditRank()
	{
		return 0;//125; // Captain
	}

	@ConfigItem(
		keyName = "enableApiSync",
		name = "Enable API Sync",
		description = "Sync diaries and progress to backend API (requires API endpoint and key)",
		section = diarySection,
		position = 2
	)
	default boolean enableApiSync()
	{
		return false;
	}

	@ConfigItem(
		keyName = "apiEndpoint",
		name = "API Endpoint",
		description = "API endpoint URL (e.g., http://localhost:3000)",
		section = diarySection,
		position = 3
	)
	default String apiEndpoint()
	{
		return "http://localhost:3000";
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API Key",
		description = "API key for authentication (get from server admin)",
		section = diarySection,
		position = 4,
		secret = true
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "clanDiaries",
		name = "",
		description = "",
		hidden = true
	)
	default String clanDiaries()
	{
		return "";
	}
}
