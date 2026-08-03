package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.Stage;
import com.doxa.crm.domain.entity.StageHistory;
import com.doxa.crm.domain.entity.User;
import com.doxa.crm.domain.enums.OpportunitySource;
import com.doxa.crm.domain.entity.OpportunityCall;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.domain.enums.UserRole;
import com.doxa.crm.dto.common.PageResponse;
import com.doxa.crm.dto.opportunity.CreateOpportunityRequest;
import com.doxa.crm.dto.opportunity.MoveStageRequest;
import com.doxa.crm.dto.opportunity.OpportunityResponse;
import com.doxa.crm.dto.opportunity.UpdateOpportunityRequest;
import com.doxa.crm.exception.AccessDeniedException;
import com.doxa.crm.exception.ResourceNotFoundException;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.OpportunityCallRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.StageHistoryRepository;
import com.doxa.crm.repository.UserRepository;
import com.doxa.crm.repository.spec.CrmSpecifications;
import com.doxa.crm.security.AuthUser;
import com.doxa.crm.security.RolePolicy;
import com.doxa.crm.util.CrmMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityCallRepository opportunityCallRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final PipelineService pipelineService;
    private final TelegramNotificationService telegramNotificationService;

    @Transactional(readOnly = true)
    public PageResponse<OpportunityResponse> list(
            AuthUser user,
            String stageSlug,
            String search,
            OpportunityStatus status,
            int page,
            int size
    ) {
        Specification<Opportunity> spec = Specification
                .where(CrmSpecifications.opportunityBelongsToLicense(user.getLicenseId()))
                .and(CrmSpecifications.opportunityMatchesRole(user))
                .and(CrmSpecifications.opportunityInStageSlug(stageSlug))
                .and(CrmSpecifications.opportunitySearch(search))
                .and(CrmSpecifications.opportunityStatus(status != null ? status : OpportunityStatus.OPEN))
                .and(CrmSpecifications.fetchContactAndStage());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Opportunity> pageResult = opportunityRepository.findAll(spec, pageable);
        Map<UUID, OpportunityCall> callsByOpportunity = loadCalls(pageResult.getContent());

        Page<OpportunityResponse> result = pageResult.map(opp ->
                CrmMapper.toOpportunityResponse(opp, callsByOpportunity.get(opp.getId())));

        return PageResponse.from(result);
    }

    private Map<UUID, OpportunityCall> loadCalls(List<Opportunity> opportunities) {
        if (opportunities.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = opportunities.stream().map(Opportunity::getId).toList();
        return opportunityCallRepository.findByOpportunity_IdIn(ids).stream()
                .collect(Collectors.toMap(call -> call.getOpportunity().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public OpportunityResponse getById(AuthUser user, UUID id) {
        Opportunity opportunity = findOwned(id, user.getLicenseId());
        verifyAccess(opportunity, user);
        OpportunityCall call = opportunityCallRepository.findByOpportunity_Id(id).orElse(null);
        return CrmMapper.toOpportunityResponse(opportunity, call);
    }

    @Transactional
    public OpportunityResponse create(AuthUser user, CreateOpportunityRequest request) {
        Contact contact = contactRepository.findByIdAndLicenseId(request.contactId(), user.getLicenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        Stage stage = pipelineService.resolveStage(user, request.stageSlug());
        User assignedUser = resolveAssignedUser(user, request.assignedUserId());

        Opportunity opportunity = Opportunity.builder()
                .license(contact.getLicense())
                .contact(contact)
                .stage(stage)
                .value(request.value() != null ? request.value() : java.math.BigDecimal.ZERO)
                .adSet(request.adSet())
                .revenueMonthly(request.revenueMonthly())
                .source(OpportunitySource.MANUAL)
                .assignedUser(assignedUser)
                .status(OpportunityStatus.OPEN)
                .build();

        opportunity = opportunityRepository.save(opportunity);

        User currentUser = userRepository.findById(user.getId()).orElseThrow();
        stageHistoryRepository.save(StageHistory.builder()
                .opportunity(opportunity)
                .fromStage(null)
                .toStage(stage)
                .changedBy(currentUser)
                .build());

        return CrmMapper.toOpportunityResponse(opportunity);
    }

    @Transactional
    public OpportunityResponse update(AuthUser user, UUID id, UpdateOpportunityRequest request) {
        Opportunity opportunity = findOwned(id, user.getLicenseId());
        verifyAccess(opportunity, user);

        if (request.stageSlug() != null && !request.stageSlug().isBlank()) {
            Stage newStage = pipelineService.resolveStage(user, request.stageSlug());
            moveToStage(opportunity, newStage, user);
        }
        if (request.value() != null) {
            opportunity.setValue(request.value());
        }
        if (request.adSet() != null) {
            opportunity.setAdSet(request.adSet());
        }
        if (request.revenueMonthly() != null) {
            opportunity.setRevenueMonthly(request.revenueMonthly());
        }
        if (request.assignedUserId() != null) {
            opportunity.setAssignedUser(resolveAssignedUser(user, request.assignedUserId()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            OpportunityStatus newStatus = OpportunityStatus.valueOf(request.status().toUpperCase());
            opportunity.setStatus(newStatus);
            if (newStatus == OpportunityStatus.WON) {
                telegramNotificationService.notifyWon(
                        user.getLicenseId(),
                        opportunity.getContact().getName()
                );
            }
        }
        if (request.lostReason() != null) {
            opportunity.setLostReason(request.lostReason());
        }

        return CrmMapper.toOpportunityResponse(
                opportunityRepository.save(opportunity),
                opportunityCallRepository.findByOpportunity_Id(id).orElse(null)
        );
    }

    @Transactional
    public OpportunityResponse moveStage(AuthUser user, UUID id, MoveStageRequest request) {
        Opportunity opportunity = findOwned(id, user.getLicenseId());
        verifyAccess(opportunity, user);

        Stage newStage = pipelineService.resolveStage(user, request.stageSlug());
        moveToStage(opportunity, newStage, user);

        if ("triage-no-show".equals(newStage.getSlug())) {
            telegramNotificationService.notifyNoShow(
                    user.getLicenseId(),
                    opportunity.getContact().getName()
            );
        }

        return CrmMapper.toOpportunityResponse(
                opportunityRepository.save(opportunity),
                opportunityCallRepository.findByOpportunity_Id(id).orElse(null)
        );
    }

    @Transactional
    public void delete(AuthUser user, UUID id) {
        Opportunity opportunity = findOwned(id, user.getLicenseId());
        verifyAccess(opportunity, user);
        opportunityRepository.delete(opportunity);
    }

    private void moveToStage(Opportunity opportunity, Stage newStage, AuthUser user) {
        Stage currentStage = opportunity.getStage();
        if (currentStage.getId().equals(newStage.getId())) {
            return;
        }

        opportunity.setStage(newStage);
        User currentUser = userRepository.findById(user.getId()).orElseThrow();
        stageHistoryRepository.save(StageHistory.builder()
                .opportunity(opportunity)
                .fromStage(currentStage)
                .toStage(newStage)
                .changedBy(currentUser)
                .build());
    }

    private User resolveAssignedUser(AuthUser user, UUID assignedUserId) {
        if (assignedUserId == null) {
            return null;
        }
        User assigned = userRepository.findById(assignedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!assigned.getLicense().getId().equals(user.getLicenseId())) {
            throw new AccessDeniedException("Cannot assign user from another tenant");
        }
        return assigned;
    }

    private Opportunity findOwned(UUID id, UUID licenseId) {
        return opportunityRepository.findByIdAndLicenseId(id, licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
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
