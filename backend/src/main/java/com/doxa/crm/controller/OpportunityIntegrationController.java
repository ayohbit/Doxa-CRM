package com.doxa.crm.controller;

import com.doxa.crm.dto.integration.CallAnalysisRequest;
import com.doxa.crm.dto.integration.CallAnalysisResponse;
import com.doxa.crm.dto.integration.TimelineEventResponse;
import com.doxa.crm.dto.integration.WrapUpRequest;
import com.doxa.crm.dto.integration.WrapUpResponse;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.OpportunityIntegrationService;
import com.doxa.crm.service.TimelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OpportunityIntegrationController {

    private final OpportunityIntegrationService integrationService;
    private final TimelineService timelineService;

    @PostMapping("/opportunities/{id}/wrap-up")
    public WrapUpResponse wrapUp(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @Valid @RequestBody WrapUpRequest request
    ) {
        return integrationService.submitWrapUp(user, id, request);
    }

    @PostMapping("/opportunities/{id}/call-analysis")
    public CallAnalysisResponse callAnalysis(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @Valid @RequestBody CallAnalysisRequest request
    ) {
        return integrationService.analyzeCall(user, id, request);
    }

    @GetMapping("/contacts/{id}/timeline")
    public List<TimelineEventResponse> timeline(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id
    ) {
        return timelineService.listForContact(id);
    }
}
