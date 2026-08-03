package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.OpportunityCall;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.TimelineEventType;
import com.doxa.crm.domain.enums.UserRole;
import com.doxa.crm.dto.integration.CallAnalysisRequest;
import com.doxa.crm.dto.integration.CallAnalysisResponse;
import com.doxa.crm.dto.integration.WrapUpRequest;
import com.doxa.crm.dto.integration.WrapUpResponse;
import com.doxa.crm.exception.AccessDeniedException;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.OpportunityCallRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpportunityIntegrationService {

    private static final String[] ANALYSIS_DIMENSIONS = {
            "rapport", "discovery", "pain_identification", "solution_fit",
            "objection_handling", "urgency", "next_steps", "closing"
    };

    private final OpportunityRepository opportunityRepository;
    private final OpportunityCallRepository opportunityCallRepository;
    private final UserRepository userRepository;
    private final TimelineService timelineService;
    private final FathomClient fathomClient;

    @Transactional
    public WrapUpResponse submitWrapUp(AuthUser user, UUID opportunityId, WrapUpRequest request) {
        Opportunity opportunity = loadAccessible(user, opportunityId);
        User currentUser = userRepository.findById(user.getId()).orElseThrow();

        OpportunityCall call = opportunityCallRepository.findByOpportunity_Id(opportunityId)
                .orElseGet(() -> OpportunityCall.builder().opportunity(opportunity).build());

        call.setOutcome(request.outcome());
        call.setObjection(request.objection());
        call.setNextStep(request.nextStep());
        call.setFilledBy(currentUser);
        opportunityCallRepository.save(call);

        timelineService.append(
                opportunity.getContact(),
                opportunity.getLicense(),
                TimelineEventType.WRAP_UP,
                "Call wrap-up: " + request.outcome(),
                buildWrapUpBody(request),
                Map.of("opportunityId", opportunityId.toString(), "outcome", request.outcome()),
                currentUser
        );

        return new WrapUpResponse(
                opportunityId,
                request.outcome(),
                request.objection(),
                request.nextStep(),
                call.getUpdatedAt()
        );
    }

    @Transactional
    public CallAnalysisResponse analyzeCall(AuthUser user, UUID opportunityId, CallAnalysisRequest request) {
        Opportunity opportunity = loadAccessible(user, opportunityId);

        String transcript = fathomClient.fetchTranscript(request.fathomUrl());
        Map<String, Integer> dimensionScores = scoreDimensions(transcript);
        BigDecimal average = dimensionScores.values().stream()
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dimensionScores.size()), 2, RoundingMode.HALF_UP);

        String summary = buildSummary(dimensionScores, average);

        OpportunityCall call = opportunityCallRepository.findByOpportunity_Id(opportunityId)
                .orElseGet(() -> OpportunityCall.builder().opportunity(opportunity).build());
        call.setFathomUrl(request.fathomUrl());
        call.setAiScore(average);
        call.setAiSummary(summary);
        opportunityCallRepository.save(call);

        User currentUser = userRepository.findById(user.getId()).orElseThrow();
        timelineService.append(
                opportunity.getContact(),
                opportunity.getLicense(),
                TimelineEventType.CALL_ANALYSIS,
                "Fathom analysis (score " + average + ")",
                summary,
                Map.of(
                        "fathomUrl", request.fathomUrl(),
                        "aiScore", average.toPlainString(),
                        "dimensions", dimensionScores
                ),
                currentUser
        );

        return new CallAnalysisResponse(
                opportunityId,
                request.fathomUrl(),
                average,
                summary,
                dimensionScores
        );
    }

    private String buildWrapUpBody(WrapUpRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Outcome: ").append(request.outcome());
        if (request.objection() != null && !request.objection().isBlank()) {
            sb.append("\nObjection: ").append(request.objection());
        }
        if (request.nextStep() != null && !request.nextStep().isBlank()) {
            sb.append("\nNext step: ").append(request.nextStep());
        }
        return sb.toString();
    }

    private Map<String, Integer> scoreDimensions(String transcript) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        int length = transcript == null ? 0 : transcript.length();
        for (int i = 0; i < ANALYSIS_DIMENSIONS.length; i++) {
            int base = 3;
            if (length > 500) {
                base++;
            }
            if (length > 1500) {
                base++;
            }
            if (transcript != null && transcript.toLowerCase().contains(ANALYSIS_DIMENSIONS[i].replace("_", " "))) {
                base = Math.min(5, base + 1);
            }
            scores.put(ANALYSIS_DIMENSIONS[i], Math.min(5, Math.max(1, base + (i % 2))));
        }
        return scores;
    }

    private String buildSummary(Map<String, Integer> scores, BigDecimal average) {
        String weakest = scores.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("discovery");
        return "Overall score " + average + "/5. Strongest areas tracked across 8 dimensions. "
                + "Focus improvement on " + weakest.replace('_', ' ') + ".";
    }

    private Opportunity loadAccessible(AuthUser user, UUID opportunityId) {
        Opportunity opportunity = opportunityRepository.findByIdAndLicenseId(opportunityId, user.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
        verifyAccess(opportunity, user);
        return opportunity;
    }

    private void verifyAccess(Opportunity opportunity, AuthUser user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.CLOSER
                && opportunity.getAssignedUser() != null
                && opportunity.getAssignedUser().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == UserRole.SDR && RolePolicy.SDR_STAGE_SLUGS.contains(opportunity.getStage().getSlug())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this opportunity");
    }
}
