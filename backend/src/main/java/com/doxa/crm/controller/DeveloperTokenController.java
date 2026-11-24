package com.doxa.crm.controller;

import com.doxa.crm.dto.developer.ApiTokenInspectionResponse;
import com.doxa.crm.dto.developer.ApiTokenResponse;
import com.doxa.crm.dto.developer.CreateApiTokenRequest;
import com.doxa.crm.dto.developer.CreatedApiTokenResponse;
import com.doxa.crm.dto.developer.InspectApiTokenRequest;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.ApiTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/developer/tokens")
@RequiredArgsConstructor
public class DeveloperTokenController {

    private final ApiTokenService apiTokenService;

    @GetMapping("/scopes")
    public Set<String> scopes(@AuthenticationPrincipal AuthUser user) {
        com.doxa.crm.security.RolePolicy.requireAdmin(user);
        return ApiTokenService.AVAILABLE_SCOPES;
    }

    @GetMapping
    public List<ApiTokenResponse> list(@AuthenticationPrincipal AuthUser user) {
        return apiTokenService.list(user);
    }

    @PostMapping
    public ResponseEntity<CreatedApiTokenResponse> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateApiTokenRequest request
    ) {
        return ResponseEntity.status(201).body(apiTokenService.create(user, request));
    }

    @DeleteMapping("/{id}")
    public ApiTokenResponse revoke(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id
    ) {
        return apiTokenService.revoke(user, id);
    }

    @PostMapping("/inspect")
    public ApiTokenInspectionResponse inspect(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody InspectApiTokenRequest request
    ) {
        return apiTokenService.inspect(user, request.token());
    }
}
