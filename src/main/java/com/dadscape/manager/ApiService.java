package com.dadscape.manager;

import com.dadscape.DadScapeConfig;
import com.dadscape.model.ClanDiary;
import com.dadscape.model.UserProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles HTTP communication with the DadScape backend API
 * Uses Java 11's HttpClient for async, non-blocking requests
 */
@Slf4j
@Singleton
public class ApiService
{
	private final DadScapeConfig config;
	private final Gson gson;
	private final HttpClient httpClient;

	@Inject
	public ApiService(DadScapeConfig config, Gson gson)
	{
		this.config = config;
		this.gson = gson;

		// Create HttpClient with timeout and connection pooling
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();
	}

	/**
	 * Check if API is configured and enabled
	 */
	public boolean isConfigured()
	{
		return config.enableApiSync()
			&& config.apiEndpoint() != null
			&& !config.apiEndpoint().isEmpty()
			&& config.apiKey() != null
			&& !config.apiKey().isEmpty();
	}

	/**
	 * Fetch all diaries from API
	 */
	public CompletableFuture<List<ClanDiary>> fetchDiaries()
	{
		if (!isConfigured())
		{
			return CompletableFuture.completedFuture(new ArrayList<>());
		}

		String url = config.apiEndpoint() + "/api/diaries";
		log.debug("Fetching diaries from: {}", url);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Authorization", "Bearer " + config.apiKey())
			.header("Content-Type", "application/json")
			.GET()
			.timeout(Duration.ofSeconds(10))
			.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() == 200)
				{
					try
					{
						ApiResponse<List<ClanDiary>> apiResponse = parseApiResponse(
							response.body(),
							new TypeToken<ApiResponse<List<ClanDiary>>>(){}.getType()
						);

						if (apiResponse != null && apiResponse.success && apiResponse.data != null)
						{
							log.info("Fetched {} diaries from API", apiResponse.data.size());
							return apiResponse.data;
						}
					}
					catch (Exception e)
					{
						log.error("Failed to parse diaries response", e);
					}
				}
				else
				{
					log.warn("Failed to fetch diaries: HTTP {}", response.statusCode());
				}
				return new ArrayList<ClanDiary>();
			})
			.exceptionally(ex -> {
				log.error("Error fetching diaries from API", ex);
				return new ArrayList<ClanDiary>();
			});
	}

	/**
	 * Create a new diary via API
	 */
	public CompletableFuture<ClanDiary> createDiary(ClanDiary diary, String rsn)
	{
		if (!isConfigured())
		{
			return CompletableFuture.completedFuture(null);
		}

		String url = config.apiEndpoint() + "/api/diaries";
		log.debug("Creating diary via API: {}", diary.getName());

		// Build request body
		Map<String, Object> body = new HashMap<>();
		body.put("name", diary.getName());
		body.put("category", diary.getCategory());
		body.put("description", diary.getDescription());
		body.put("createdBy", diary.getCreatedBy());
		body.put("rsn", rsn);

		String jsonBody = gson.toJson(body);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Authorization", "Bearer " + config.apiKey())
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
			.timeout(Duration.ofSeconds(10))
			.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() == 201 || response.statusCode() == 200)
				{
					try
					{
						ApiResponse<ClanDiary> apiResponse = parseApiResponse(
							response.body(),
							new TypeToken<ApiResponse<ClanDiary>>(){}.getType()
						);

						if (apiResponse != null && apiResponse.success && apiResponse.data != null)
						{
							log.info("Created diary via API: {}", apiResponse.data.getName());
							return apiResponse.data;
						}
					}
					catch (Exception e)
					{
						log.error("Failed to parse create diary response", e);
					}
				}
				else
				{
					log.warn("Failed to create diary: HTTP {} - {}", response.statusCode(), response.body());
				}
				return null;
			})
			.exceptionally(ex -> {
				log.error("Error creating diary via API", ex);
				return null;
			});
	}

	/**
	 * Update an existing diary via API
	 */
	public CompletableFuture<ClanDiary> updateDiary(ClanDiary diary, String rsn)
	{
		if (!isConfigured())
		{
			return CompletableFuture.completedFuture(null);
		}

		String url = config.apiEndpoint() + "/api/diaries/" + diary.getId();
		log.debug("Updating diary via API: {}", diary.getName());

		// Build request body with all fields
		Map<String, Object> body = new HashMap<>();
		body.put("name", diary.getName());
		body.put("description", diary.getDescription());
		body.put("category", diary.getCategory());
		body.put("version", diary.getVersion());
		body.put("tiers", diary.getTiers());
		body.put("active", diary.isActive());
		body.put("lastModifiedBy", diary.getLastModifiedBy());
		body.put("rsn", rsn);

		String jsonBody = gson.toJson(body);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Authorization", "Bearer " + config.apiKey())
			.header("Content-Type", "application/json")
			.PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
			.timeout(Duration.ofSeconds(10))
			.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() == 200)
				{
					try
					{
						ApiResponse<ClanDiary> apiResponse = parseApiResponse(
							response.body(),
							new TypeToken<ApiResponse<ClanDiary>>(){}.getType()
						);

						if (apiResponse != null && apiResponse.success && apiResponse.data != null)
						{
							log.info("Updated diary via API: {}", apiResponse.data.getName());
							return apiResponse.data;
						}
					}
					catch (Exception e)
					{
						log.error("Failed to parse update diary response", e);
					}
				}
				else
				{
					log.warn("Failed to update diary: HTTP {} - {}", response.statusCode(), response.body());
				}
				return null;
			})
			.exceptionally(ex -> {
				log.error("Error updating diary via API", ex);
				return null;
			});
	}

	/**
	 * Delete a diary via API
	 */
	public CompletableFuture<Boolean> deleteDiary(String diaryId, String rsn)
	{
		if (!isConfigured())
		{
			return CompletableFuture.completedFuture(false);
		}

		String url = config.apiEndpoint() + "/api/diaries/" + diaryId + "?rsn=" + rsn;
		log.debug("Deleting diary via API: {}", diaryId);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Authorization", "Bearer " + config.apiKey())
			.DELETE()
			.timeout(Duration.ofSeconds(10))
			.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() == 200)
				{
					log.info("Deleted diary via API: {}", diaryId);
					return true;
				}
				else
				{
					log.warn("Failed to delete diary: HTTP {} - {}", response.statusCode(), response.body());
					return false;
				}
			})
			.exceptionally(ex -> {
				log.error("Error deleting diary via API", ex);
				return false;
			});
	}

	/**
	 * Fetch message of the day from API
	 */
	public CompletableFuture<String> fetchMotd()
	{
		if (!isConfigured())
		{
			return CompletableFuture.completedFuture("");
		}

		String url = config.apiEndpoint() + "/api/motd";
		log.debug("Fetching MOTD from API");

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Authorization", "Bearer " + config.apiKey())
			.GET()
			.timeout(Duration.ofSeconds(10))
			.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() == 200)
				{
					try
					{
						ApiResponse<String> apiResponse = parseApiResponse(
							response.body(),
							new TypeToken<ApiResponse<String>>(){}.getType()
						);

						if (apiResponse != null && apiResponse.success && apiResponse.data != null)
						{
							log.debug("Fetched MOTD from API");
							return apiResponse.data;
						}
					}
					catch (Exception e)
					{
						log.error("Failed to parse MOTD response", e);
					}
				}
				return "";
			})
			.exceptionally(ex -> {
				log.error("Error fetching MOTD from API", ex);
				return "";
			});
	}

	/**
	 * Update message of the day via API
	 */
	public CompletableFuture<Boolean> updateMotd(String motd, String rsn)
	{
		if (!isConfigured())
		{
			return CompletableFuture.completedFuture(false);
		}

		String url = config.apiEndpoint() + "/api/motd";
		log.debug("Updating MOTD via API");

		Map<String, String> body = new HashMap<>();
		body.put("motd", motd);
		body.put("rsn", rsn);

		String jsonBody = gson.toJson(body);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Authorization", "Bearer " + config.apiKey())
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
			.timeout(Duration.ofSeconds(10))
			.build();

		return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenApply(response -> {
				if (response.statusCode() == 200)
				{
					log.info("Updated MOTD via API");
					return true;
				}
				else
				{
					log.warn("Failed to update MOTD: HTTP {} - {}", response.statusCode(), response.body());
					return false;
				}
			})
			.exceptionally(ex -> {
				log.error("Error updating MOTD via API", ex);
				return false;
			});
	}

	/**
	 * Parse API response JSON into ApiResponse object
	 */
	private <T> ApiResponse<T> parseApiResponse(String json, Type type)
	{
		try
		{
			return gson.fromJson(json, type);
		}
		catch (Exception e)
		{
			log.error("Failed to parse API response", e);
			return null;
		}
	}

	/**
	 * Inner class representing API response structure
	 */
	private static class ApiResponse<T>
	{
		public boolean success;
		public T data;
		public String error;
		public String message;
	}
}
