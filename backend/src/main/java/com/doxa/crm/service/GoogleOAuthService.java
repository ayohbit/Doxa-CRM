package com.doxa.crm.service;

import com.doxa.crm.domain.entity.OAuthConnection;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.exception.IntegrationException;
import com.doxa.crm.repository.OAuthConnectionRepository;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPES = String.join(" ",
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/gmail.send");

    private final OAuthConnectionRepository oauthConnectionRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JsonMapper jsonMapper;

    @Value("${app.google.client-id:}")
    private String clientId;

    @Value("${app.google.client-secret:}")
    private String clientSecret;

    @Value("${app.google.redirect-uri:http://localhost:8080/api/integrations/google/callback}")
    private String redirectUri;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return oauthConnectionRepository.findByUserId(userId).isPresent();
    }

    public String buildAuthUrl(UUID userId) {
        if (!isConfigured()) {
            throw new IntegrationException("Google OAuth is not configured on the server");
        }
        String state = jwtService.generateOAuthState(userId);
        return AUTH_URL + "?" + buildQuery(Map.of(
                "client_id", clientId,
                "redirect_uri", redirectUri,
                "response_type", "code",
                "scope", SCOPES,
                "access_type", "offline",
                "prompt", "consent",
                "state", state
        ));
    }

    @Transactional
    public String handleCallback(String code, String state) {
        UUID userId = jwtService.parseOAuthState(state);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IntegrationException("User not found for OAuth state"));

        JsonNode tokenResponse = exchangeCode(code);
        String accessToken = tokenResponse.get("access_token").asString();
        String refreshToken = tokenResponse.hasNonNull("refresh_token")
                ? tokenResponse.get("refresh_token").asString()
                : null;
        long expiresIn = tokenResponse.has("expires_in") ? tokenResponse.get("expires_in").asLong() : 3600;

        OAuthConnection connection = oauthConnectionRepository.findByUserId(userId)
                .orElseGet(() -> OAuthConnection.builder().user(user).build());
        connection.setAccessToken(accessToken);
        if (refreshToken != null) {
            connection.setRefreshToken(refreshToken);
        }
        connection.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        connection.setScopes(SCOPES);
        oauthConnectionRepository.save(connection);

        return frontendUrl + "/opportunities?google=connected";
    }

    @Transactional
    public String getValidAccessToken(UUID userId) {
        OAuthConnection connection = oauthConnectionRepository.findByUserId(userId)
                .orElseThrow(() -> new IntegrationException("Connect your Google account first"));

        if (connection.getExpiresAt() != null
                && connection.getExpiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return connection.getAccessToken();
        }
        if (connection.getRefreshToken() == null) {
            throw new IntegrationException("Google session expired — reconnect your account");
        }

        JsonNode refreshed = refreshToken(connection.getRefreshToken());
        connection.setAccessToken(refreshed.get("access_token").asString());
        long expiresIn = refreshed.has("expires_in") ? refreshed.get("expires_in").asLong() : 3600;
        connection.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        oauthConnectionRepository.save(connection);
        return connection.getAccessToken();
    }

    private JsonNode exchangeCode(String code) {
        try {
            String body = buildQuery(Map.of(
                    "code", code,
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "redirect_uri", redirectUri,
                    "grant_type", "authorization_code"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IntegrationException("Google token exchange failed");
            }
            return jsonMapper.readTree(response.body());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationException("Google token exchange failed: " + ex.getMessage());
        }
    }

    private JsonNode refreshToken(String refreshToken) {
        try {
            String body = buildQuery(Map.of(
                    "refresh_token", refreshToken,
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "grant_type", "refresh_token"
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IntegrationException("Google token refresh failed");
            }
            return jsonMapper.readTree(response.body());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationException("Google token refresh failed: " + ex.getMessage());
        }
    }

    private String buildQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        });
        return sb.toString();
    }
}
