package com.doxa.crm.controller;

import com.doxa.crm.dto.integration.CalendarInviteRequest;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.GoogleCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;

    @PostMapping("/opportunities/{id}/calendar/invite")
    public Map<String, String> invite(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @Valid @RequestBody CalendarInviteRequest request
    ) {
        return googleCalendarService.createInvite(user, id, request);
    }
}
