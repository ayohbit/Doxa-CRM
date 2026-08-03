package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.TimelineEventType;
import com.doxa.crm.dto.integration.SendEmailRequest;
import com.doxa.crm.exception.IntegrationException;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GmailService {

    private final GoogleOAuthService googleOAuthService;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;
    private final JsonMapper jsonMapper;

    @Transactional
    public Map<String, String> sendEmail(AuthUser user, UUID contactId, SendEmailRequest request) {
        Contact contact = contactRepository.findByIdAndLicenseId(contactId, user.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        if (contact.getEmail() == null || contact.getEmail().isBlank()) {
            throw new IntegrationException("Contact email is required");
        }

        String accessToken = googleOAuthService.getValidAccessToken(user.getId());
        String rawMessage = buildRawEmail(user.getEmail(), contact.getEmail(), request.subject(), request.body());
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rawMessage.getBytes(StandardCharsets.UTF_8));

        try {
            String payload = jsonMapper.writeValueAsString(Map.of("raw", encoded));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IntegrationException("Gmail API error: " + response.body());
            }

            User currentUser = userRepository.findById(user.getId()).orElseThrow();
            timelineService.append(
                    contact,
                    contact.getLicense(),
                    TimelineEventType.EMAIL_SENT,
                    request.subject(),
                    request.body(),
                    Map.of("contactId", contactId.toString()),
                    currentUser
            );

            return Map.of("status", "sent", "message", "Email sent to " + contact.getEmail());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationException("Failed to send email: " + ex.getMessage());
        }
    }

    private String buildRawEmail(String from, String to, String subject, String body) {
        return "From: " + from + "\r\n"
                + "To: " + to + "\r\n"
                + "Subject: " + subject + "\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n\r\n"
                + body;
    }
}
