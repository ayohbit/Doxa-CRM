package com.doxa.crm.controller;

import com.doxa.crm.dto.integration.GoogleAuthUrlResponse;
import com.doxa.crm.dto.integration.IntegrationStatusResponse;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final GoogleOAuthService googleOAuthService;

    @Value("${app.telegram.bot-token:}")
    private String telegramBotToken;

    @GetMapping("/status")
    public IntegrationStatusResponse status(@AuthenticationPrincipal AuthUser user) {
        return new IntegrationStatusResponse(
                googleOAuthService.isConnected(user.getId()),
                googleOAuthService.isConfigured(),
                telegramBotToken != null && !telegramBotToken.isBlank(),
                null
        );
    }

    @GetMapping("/google/auth-url")
    public GoogleAuthUrlResponse googleAuthUrl(@AuthenticationPrincipal AuthUser user) {
        return new GoogleAuthUrlResponse(googleOAuthService.buildAuthUrl(user.getId()));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        String redirect = googleOAuthService.handleCallback(code, state);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(redirect));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
