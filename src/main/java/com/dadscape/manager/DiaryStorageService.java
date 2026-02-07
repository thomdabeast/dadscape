package com.dadscape.manager;

import com.dadscape.model.ClanDiary;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence of clan diaries to ConfigManager and file system.
 * Supports both local storage and export/import functionality.
 */
@Slf4j
@Singleton
public class DiaryStorageService
{
	private static final String CONFIG_GROUP = "dadscape";
	private static final String DIARIES_KEY = "clanDiaries";
	private static final String MOTD_KEY = "messageOfTheDay";
	private static final String EXPORT_DIR = ".runelite/dadscape/diaries";

	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	public DiaryStorageService(ConfigManager configManager)
	{
		this.configManager = configManager;
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
	}

	/**
	 * Load all diaries from ConfigManager
	 */
	public List<ClanDiary> loadDiaries()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, DIARIES_KEY);
		if (json == null || json.isEmpty())
		{
			log.debug("No diaries found in config, returning empty list");
			return new ArrayList<>();
		}

		try
		{
			Type listType = new TypeToken<List<ClanDiary>>(){}.getType();
			List<ClanDiary> diaries = gson.fromJson(json, listType);
			return diaries != null ? diaries : new ArrayList<>();
		}
		catch (Exception e)
		{
			log.error("Failed to deserialize diaries from config", e);
			return new ArrayList<>();
		}
	}

	/**
	 * Save all diaries to ConfigManager
	 */
	public void saveDiaries(List<ClanDiary> diaries)
	{
		try
		{
			String json = gson.toJson(diaries);
			configManager.setConfiguration(CONFIG_GROUP, DIARIES_KEY, json);
			log.debug("Saved {} diaries to config", diaries.size());
		}
		catch (Exception e)
		{
			log.error("Failed to serialize diaries to config", e);
		}
	}

	/**
	 * Export a diary to a JSON file
	 */
	public boolean exportDiary(ClanDiary diary, String fileName) throws IOException
	{
		Path exportPath = getExportPath();
		Files.createDirectories(exportPath);

		// Sanitize filename
		String sanitized = fileName.replaceAll("[^a-zA-Z0-9-_]", "_");
		Path filePath = exportPath.resolve(sanitized + ".json");

		String json = gson.toJson(diary);
		Files.write(filePath, json.getBytes());

		log.info("Exported diary '{}' to {}", diary.getName(), filePath);
		return true;
	}

	/**
	 * Import a diary from a JSON file
	 */
	public ClanDiary importDiary(Path filePath) throws IOException
	{
		if (!Files.exists(filePath))
		{
			throw new IOException("File not found: " + filePath);
		}

		String json = new String(Files.readAllBytes(filePath));
		ClanDiary diary = gson.fromJson(json, ClanDiary.class);

		if (diary == null)
		{
			throw new IOException("Failed to parse diary JSON");
		}

		log.info("Imported diary '{}' from {}", diary.getName(), filePath);
		return diary;
	}

	/**
	 * Import a diary from JSON string
	 */
	public ClanDiary importDiaryFromJson(String json)
	{
		try
		{
			ClanDiary diary = gson.fromJson(json, ClanDiary.class);
			if (diary == null)
			{
				throw new IllegalArgumentException("Invalid diary JSON");
			}
			return diary;
		}
		catch (Exception e)
		{
			log.error("Failed to import diary from JSON", e);
			throw new IllegalArgumentException("Invalid diary JSON", e);
		}
	}

	/**
	 * Export a diary to JSON string
	 */
	public String exportDiaryToJson(ClanDiary diary)
	{
		return gson.toJson(diary);
	}

	/**
	 * Get the export directory path
	 */
	private Path getExportPath()
	{
		String userHome = System.getProperty("user.home");
		return Paths.get(userHome, EXPORT_DIR);
	}

	/**
	 * Get the export directory as a File
	 */
	public File getExportDirectory()
	{
		return getExportPath().toFile();
	}

	/**
	 * List all exported diary files
	 */
	public List<File> listExportedDiaries()
	{
		List<File> files = new ArrayList<>();
		File exportDir = getExportDirectory();

		if (!exportDir.exists())
		{
			return files;
		}

		File[] jsonFiles = exportDir.listFiles((dir, name) -> name.endsWith(".json"));
		if (jsonFiles != null)
		{
			for (File file : jsonFiles)
			{
				files.add(file);
			}
		}

		return files;
	}

	/**
	 * Load message of the day from ConfigManager
	 */
	public String loadMotd()
	{
		String motd = configManager.getConfiguration(CONFIG_GROUP, MOTD_KEY);
		return motd != null ? motd : "";
	}

	/**
	 * Save message of the day to ConfigManager
	 */
	public void saveMotd(String motd)
	{
		try
		{
			configManager.setConfiguration(CONFIG_GROUP, MOTD_KEY, motd != null ? motd : "");
			log.debug("Saved MOTD to config");
		}
		catch (Exception e)
		{
			log.error("Failed to save MOTD to config", e);
		}
	}
}
