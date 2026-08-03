package com.doxa.crm.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeadBrokerPayload(
        String event,
        @JsonProperty("broker_lead_id") String brokerLeadId,
        @JsonProperty("license_id") String licenseId,
        @JsonProperty("purchased_at") Instant purchasedAt,
        @JsonProperty("price_paid") BigDecimal pricePaid,
        String currency,
        LeadBrokerSource source,
        LeadBrokerLead lead
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeadBrokerSource(
            String campaign,
            String platform
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeadBrokerLead(
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String email,
            String phone,
            @JsonProperty("revenue_monthly") String revenueMonthly,
            LeadBrokerConsent consent,
            @JsonProperty("custom_fields") Map<String, Object> customFields
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeadBrokerConsent(
            @JsonProperty("tcpa_opt_in") Boolean tcpaOptIn,
            @JsonProperty("collected_at") Instant collectedAt
    ) {
    }
}
