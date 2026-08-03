package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.TimelineEventType;
import com.doxa.crm.dto.integration.CalendarInviteRequest;
import com.doxa.crm.exception.IntegrationException;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.OpportunityRepository;
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
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final GoogleOAuthService googleOAuthService;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;
    private final JsonMapper jsonMapper;

    @Transactional
    public Map<String, String> createInvite(AuthUser user, UUID opportunityId, CalendarInviteRequest request) {
        Opportunity opportunity = opportunityRepository.findByIdAndLicenseId(opportunityId, user.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));

        Contact contact = opportunity.getContact();
        if (contact.getEmail() == null || contact.getEmail().isBlank()) {
            throw new IntegrationException("Contact email is required for calendar invite");
        }

        String accessToken = googleOAuthService.getValidAccessToken(user.getId());
        Instant end = request.startAt().plusSeconds(request.resolvedDurationMinutes() * 60L);
        String title = request.title() != null && !request.title().isBlank()
                ? request.title()
                : "Meeting with " + contact.getName();

        try {
            String payload = jsonMapper.writeValueAsString(Map.of(
                    "summary", title,
                    "start", Map.of("dateTime", request.startAt().toString()),
                    "end", Map.of("dateTime", end.toString()),
                    "attendees", new Object[]{Map.of("email", contact.getEmail())}
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.googleapis.com/calendar/v3/calendars/primary/events?sendUpdates=all"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IntegrationException("Google Calendar API error: " + response.body());
            }

            User currentUser = userRepository.findById(user.getId()).orElseThrow();
            timelineService.append(
                    contact,
                    opportunity.getLicense(),
                    TimelineEventType.CALENDAR_INVITE,
                    "Calendar invite: " + title,
                    "Scheduled for " + request.startAt(),
                    Map.of("opportunityId", opportunityId.toString(), "startAt", request.startAt().toString()),
                    currentUser
            );

            return Map.of("status", "created", "message", "Calendar invite sent to " + contact.getEmail());
        } catch (IntegrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IntegrationException("Failed to create calendar event: " + ex.getMessage());
        }
    }
}
