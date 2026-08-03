package com.doxa.crm.service;

import com.doxa.crm.repository.TelegramSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final TelegramSettingsRepository telegramSettingsRepository;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    public void notifyLicense(UUID licenseId, String message) {
        if (botToken == null || botToken.isBlank()) {
            log.debug("Telegram bot token not configured; skipping alert");
            return;
        }

        telegramSettingsRepository.findByLicenseIdAndEnabledTrue(licenseId).ifPresent(settings -> {
            try {
                String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
                URI uri = URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id="
                        + settings.getChatId() + "&text=" + encoded);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400) {
                    log.warn("Telegram API returned {}: {}", response.statusCode(), response.body());
                }
            } catch (Exception ex) {
                log.warn("Failed to send Telegram notification: {}", ex.getMessage());
            }
        });
    }

    public void notifyNewLead(UUID licenseId, String leadName, String brokerLeadId) {
        notifyLicense(licenseId, "New lead: " + leadName + " (" + brokerLeadId + ")");
    }

    public void notifyNoShow(UUID licenseId, String opportunityName) {
        notifyLicense(licenseId, "No-show: " + opportunityName);
    }

    public void notifyWon(UUID licenseId, String opportunityName) {
        notifyLicense(licenseId, "Opportunity won: " + opportunityName);
    }
}
