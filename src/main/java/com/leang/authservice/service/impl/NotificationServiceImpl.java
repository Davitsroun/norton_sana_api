package com.leang.authservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leang.authservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final String ONE_SIGNAL_URL = "https://onesignal.com/api/v1/notifications";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(@Qualifier("httpClient") RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${onesignal.app-id}")
    private String appId;

    @Value("${onesignal.rest-api-key}")
    private String restApiKey;

    @Override
    public void sendMessageToAllUsers(String message) {
        validateConfig();
        String body = buildJsonBodyForAllUsers(message);
        HttpEntity<String> request = new HttpEntity<>(body, buildHeaders());
        restTemplate.postForEntity(ONE_SIGNAL_URL, request, String.class);
    }

    @Override
    public void sendMessageToUser(String userId, String message) {
        validateConfig();
        String body = buildJsonBodyForSingleUser(message, userId);
        HttpEntity<String> request = new HttpEntity<>(body, buildHeaders());
        restTemplate.postForEntity(ONE_SIGNAL_URL, request, String.class);
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Basic " + restApiKey.trim());
        return headers;
    }

    private void validateConfig() {
        String normalizedAppId = appId == null ? "" : appId.trim();
        String normalizedApiKey = restApiKey == null ? "" : restApiKey.trim();

        if (normalizedAppId.isBlank()) {
            throw new IllegalStateException("ONESIGNAL_APP_ID is missing");
        }
        if (normalizedApiKey.isBlank()) {
            throw new IllegalStateException("ONESIGNAL_REST_API_KEY is missing");
        }
        try {
            UUID.fromString(normalizedAppId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("ONESIGNAL_APP_ID must be a valid UUID");
        }
    }

    private String buildJsonBodyForAllUsers(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", appId.trim());
        body.put("included_segments", new String[]{"All"});
        Map<String, String> data = new HashMap<>();
        data.put("foo", "bar");
        body.put("data", data);
        Map<String, String> contents = new HashMap<>();
        contents.put("en", message);
        body.put("contents", contents);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OneSignal payload", e);
        }
    }

    private String buildJsonBodyForSingleUser(String message, String userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", appId.trim());
        body.put("target_channel", "push");
        Map<String, String[]> includeAliases = new HashMap<>();
        includeAliases.put("external_id", new String[]{userId});
        body.put("include_aliases", includeAliases);
        Map<String, String> data = new HashMap<>();
        data.put("foo", "bar");
        body.put("data", data);
        Map<String, String> contents = new HashMap<>();
        contents.put("en", message);
        body.put("contents", contents);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OneSignal payload", e);
        }
    }
}
