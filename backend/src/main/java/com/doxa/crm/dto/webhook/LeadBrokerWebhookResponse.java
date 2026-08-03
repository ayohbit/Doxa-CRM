package com.doxa.crm.dto.webhook;

import java.util.UUID;

public record LeadBrokerWebhookResponse(
        UUID opportunityId,
        UUID contactId,
        String brokerLeadId,
        boolean created,
        String message
) {
}
