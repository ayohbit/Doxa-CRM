package com.doxa.crm.service;

import com.doxa.crm.domain.entity.Contact;
import com.doxa.crm.domain.entity.License;
import com.doxa.crm.domain.entity.Opportunity;
import com.doxa.crm.domain.entity.Stage;
import com.doxa.crm.domain.entity.StageHistory;
import com.doxa.crm.domain.entity.WebhookLog;
import com.doxa.crm.domain.enums.LicenseStatus;
import com.doxa.crm.domain.enums.OpportunitySource;
import com.doxa.crm.domain.enums.OpportunityStatus;
import com.doxa.crm.dto.webhook.LeadBrokerPayload;
import com.doxa.crm.dto.webhook.LeadBrokerWebhookResponse;
import com.doxa.crm.exception.WebhookRejectedException;
import com.doxa.crm.repository.ContactRepository;
import com.doxa.crm.repository.LicenseRepository;
import com.doxa.crm.repository.OpportunityRepository;
import com.doxa.crm.repository.PipelineRepository;
import com.doxa.crm.repository.StageHistoryRepository;
import com.doxa.crm.repository.StageRepository;
import com.doxa.crm.repository.WebhookLogRepository;
import com.doxa.crm.util.PhoneNormalizer;
import com.doxa.crm.util.WebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadBrokerWebhookService {

    private static final String EVENT_LEAD_PURCHASED = "lead.purchased";
    private static final String DEFAULT_STAGE_SLUG = "new-lead";

    private final JsonMapper jsonMapper;
    private final LicenseRepository licenseRepository;
    private final PipelineRepository pipelineRepository;
    private final StageRepository stageRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final StageHistoryRepository stageHistoryRepository;
    private final WebhookLogRepository webhookLogRepository;

    @Transactional
    public LeadBrokerWebhookResponse handle(String rawBody, String signatureHeader) {
        Map<String, Object> rawPayload = parseRawPayload(rawBody);
        LeadBrokerPayload payload;

        try {
            payload = jsonMapper.readValue(rawBody, LeadBrokerPayload.class);
            validatePayload(payload);
        } catch (WebhookRejectedException ex) {
            logWebhook(null, rawPayload, false, "rejected", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            logWebhook(null, rawPayload, false, "rejected", "Malformed JSON payload");
            throw new WebhookRejectedException("Malformed JSON payload");
        }

        License license = resolveLicense(payload.licenseId())
                .orElseThrow(() -> {
                    logWebhook(null, rawPayload, false, "rejected", "Unknown license_id");
                    return new WebhookRejectedException("Unknown or inactive license_id");
                });

        if (license.getStatus() != LicenseStatus.ACTIVE) {
            logWebhook(license, rawPayload, false, "rejected", "License suspended");
            throw new WebhookRejectedException("License is not active");
        }

        boolean signatureValid = WebhookSignatureVerifier.isValid(
                license.getWebhookSecret(),
                rawBody,
                signatureHeader
        );

        if (!signatureValid) {
            logWebhook(license, rawPayload, false, "rejected", "Invalid signature");
            throw new WebhookRejectedException("Invalid webhook signature");
        }

        Optional<Opportunity> existing = opportunityRepository.findByLicenseIdAndBrokerLeadId(
                license.getId(),
                payload.brokerLeadId()
        );

        if (existing.isPresent()) {
            Opportunity opportunity = existing.get();
            logWebhook(license, rawPayload, true, "duplicate", null);
            return new LeadBrokerWebhookResponse(
                    opportunity.getId(),
                    opportunity.getContact().getId(),
                    payload.brokerLeadId(),
                    false,
                    "Lead already processed"
            );
        }

        Stage entryStage = resolveEntryStage(license);
        Contact contact = resolveContact(license, payload);
        Opportunity opportunity = createOpportunity(license, contact, entryStage, payload);

        stageHistoryRepository.save(StageHistory.builder()
                .opportunity(opportunity)
                .fromStage(null)
                .toStage(entryStage)
                .build());

        logWebhook(license, rawPayload, true, "processed", null);

        return new LeadBrokerWebhookResponse(
                opportunity.getId(),
                contact.getId(),
                payload.brokerLeadId(),
                true,
                "Lead processed successfully"
        );
    }

    private Map<String, Object> parseRawPayload(String rawBody) {
        try {
            return jsonMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of("raw", rawBody);
        }
    }

    private void validatePayload(LeadBrokerPayload payload) {
        if (payload.event() == null || !EVENT_LEAD_PURCHASED.equals(payload.event())) {
            throw new WebhookRejectedException("Unsupported or missing event");
        }
        if (payload.brokerLeadId() == null || payload.brokerLeadId().isBlank()) {
            throw new WebhookRejectedException("broker_lead_id is required");
        }
        if (payload.licenseId() == null || payload.licenseId().isBlank()) {
            throw new WebhookRejectedException("license_id is required");
        }
        if (payload.lead() == null) {
            throw new WebhookRejectedException("lead object is required");
        }

        LeadBrokerPayload.LeadBrokerLead lead = payload.lead();
        boolean hasIdentity = (lead.email() != null && !lead.email().isBlank())
                || (lead.phone() != null && !lead.phone().isBlank());
        if (!hasIdentity) {
            throw new WebhookRejectedException("lead email or phone is required");
        }
    }

    private Stage resolveEntryStage(License license) {
        var pipeline = pipelineRepository.findByLicenseIdAndName(license.getId(), PipelineService.ADS_PIPELINE_NAME)
                .orElseThrow(() -> new WebhookRejectedException("Default pipeline not configured for tenant"));

        return stageRepository.findByPipelineIdAndSlug(pipeline.getId(), DEFAULT_STAGE_SLUG)
                .orElseThrow(() -> new WebhookRejectedException("Default entry stage not configured for tenant"));
    }

    private Contact resolveContact(License license, LeadBrokerPayload payload) {
        LeadBrokerPayload.LeadBrokerLead lead = payload.lead();
        String phoneE164 = PhoneNormalizer.toE164(lead.phone());
        String dedupeKey = PhoneNormalizer.dedupeKey(lead.email(), phoneE164);

        Optional<Contact> existing = contactRepository.findByLicenseIdAndDedupeKey(license.getId(), dedupeKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        String name = buildName(lead.firstName(), lead.lastName());
        Map<String, Object> customFields = new HashMap<>();
        if (lead.customFields() != null) {
            customFields.putAll(lead.customFields());
        }
        if (payload.source() != null) {
            customFields.put("broker_platform", payload.source().platform());
        }
        if (lead.consent() != null) {
            customFields.put("tcpa_opt_in", lead.consent().tcpaOptIn());
            if (lead.consent().collectedAt() != null) {
                customFields.put("consent_collected_at", lead.consent().collectedAt().toString());
            }
        }

        Contact contact = Contact.builder()
                .license(license)
                .name(name)
                .email(lead.email())
                .phone(lead.phone())
                .phoneE164(phoneE164)
                .dedupeKey(dedupeKey)
                .customFields(customFields)
                .build();

        return contactRepository.save(contact);
    }

    private Opportunity createOpportunity(
            License license,
            Contact contact,
            Stage stage,
            LeadBrokerPayload payload
    ) {
        String adSet = payload.source() != null ? payload.source().campaign() : null;
        BigDecimal value = payload.pricePaid() != null ? payload.pricePaid() : BigDecimal.ZERO;
        String currency = payload.currency() != null && !payload.currency().isBlank()
                ? payload.currency()
                : "USD";

        Opportunity opportunity = Opportunity.builder()
                .license(license)
                .contact(contact)
                .stage(stage)
                .value(value)
                .currency(currency)
                .adSet(adSet)
                .revenueMonthly(payload.lead().revenueMonthly())
                .source(OpportunitySource.BROKER)
                .brokerLeadId(payload.brokerLeadId())
                .status(OpportunityStatus.OPEN)
                .build();

        if (payload.purchasedAt() != null) {
            opportunity.setCreatedAt(payload.purchasedAt());
            opportunity.setUpdatedAt(payload.purchasedAt());
        }

        return opportunityRepository.save(opportunity);
    }

    private String buildName(String firstName, String lastName) {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        String combined = (fn + " " + ln).trim();
        return combined.isBlank() ? "Unknown Lead" : combined;
    }

    private Optional<License> resolveLicense(String licenseRef) {
        Optional<License> byBrokerId = licenseRepository.findByBrokerLicenseId(licenseRef);
        if (byBrokerId.isPresent()) {
            return byBrokerId;
        }
        try {
            return licenseRepository.findById(UUID.fromString(licenseRef));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private void logWebhook(
            License license,
            Map<String, Object> rawPayload,
            boolean signatureValid,
            String result,
            String errorMessage
    ) {
        webhookLogRepository.save(WebhookLog.builder()
                .license(license)
                .rawPayload(rawPayload)
                .signatureValid(signatureValid)
                .result(result)
                .errorMessage(errorMessage)
                .build());
    }
}
