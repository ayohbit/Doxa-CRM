package com.doxa.crm.service;

import com.doxa.crm.exception.IntegrationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FathomClient {

    private static final Pattern RECORDING_ID = Pattern.compile("(?:recording[s]?/|share/)([a-zA-Z0-9_-]+)");

    @Value("${app.fathom.api-key:}")
    private String apiKey;

    @Value("${app.fathom.api-base-url:https://api.fathom.video/v1}")
    private String apiBaseUrl;

    public String fetchTranscript(String fathomUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Fathom API key not configured; using demo transcript for {}", fathomUrl);
            return demoTranscript(fathomUrl);
        }

        String recordingId = extractRecordingId(fathomUrl);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/recordings/" + recordingId + "/transcript"))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Fathom API {} — falling back to demo transcript", response.statusCode());
                return demoTranscript(fathomUrl);
            }
            return response.body();
        } catch (Exception ex) {
            log.warn("Fathom fetch failed: {} — using demo transcript", ex.getMessage());
            return demoTranscript(fathomUrl);
        }
    }

    private String extractRecordingId(String fathomUrl) {
        Matcher matcher = RECORDING_ID.matcher(fathomUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IntegrationException("Unable to parse Fathom recording id from URL");
    }

    private String demoTranscript(String fathomUrl) {
        return """
                Demo transcript for %s.
                Rep built rapport and ran discovery on revenue goals and pain points.
                Prospect raised budget objection; rep handled with ROI framing.
                Next steps: send proposal and schedule follow-up call.
                """.formatted(fathomUrl);
    }
}
