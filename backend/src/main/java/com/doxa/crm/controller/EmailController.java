package com.doxa.crm.controller;

import com.doxa.crm.dto.integration.SendEmailRequest;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.GmailService;
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
public class EmailController {

    private final GmailService gmailService;

    @PostMapping("/contacts/{id}/email/send")
    public Map<String, String> send(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @Valid @RequestBody SendEmailRequest request
    ) {
        return gmailService.sendEmail(user, id, request);
    }
}
