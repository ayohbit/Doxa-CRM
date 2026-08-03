package com.doxa.crm.controller;

import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.dto.common.PageResponse;
import com.doxa.crm.dto.opportunity.CreateOpportunityRequest;
import com.doxa.crm.dto.opportunity.MoveStageRequest;
import com.doxa.crm.dto.opportunity.OpportunityResponse;
import com.doxa.crm.dto.opportunity.UpdateOpportunityRequest;
import com.doxa.crm.dto.pipeline.PipelineBoardResponse;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.service.OpportunityService;
import com.doxa.crm.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final PipelineService pipelineService;

    @GetMapping("/pipelines/board")
    public PipelineBoardResponse getBoard(@AuthenticationPrincipal AuthUser user) {
        return pipelineService.getBoard(user);
    }

    @GetMapping("/opportunities")
    public PageResponse<OpportunityResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) String stageSlug,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return opportunityService.list(user, stageSlug, q, status, page, size);
    }

    @GetMapping("/opportunities/{id}")
    public OpportunityResponse getById(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id
    ) {
        return opportunityService.getById(user, id);
    }

    @PostMapping("/opportunities")
    public ResponseEntity<OpportunityResponse> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateOpportunityRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(opportunityService.create(user, request));
    }

    @PutMapping("/opportunities/{id}")
    public OpportunityResponse update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @RequestBody UpdateOpportunityRequest request
    ) {
        return opportunityService.update(user, id, request);
    }

    @PatchMapping("/opportunities/{id}/stage")
    public OpportunityResponse moveStage(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id,
            @Valid @RequestBody MoveStageRequest request
    ) {
        return opportunityService.moveStage(user, id, request);
    }

    @DeleteMapping("/opportunities/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable UUID id
    ) {
        opportunityService.delete(user, id);
        return ResponseEntity.noContent().build();
    }
}
