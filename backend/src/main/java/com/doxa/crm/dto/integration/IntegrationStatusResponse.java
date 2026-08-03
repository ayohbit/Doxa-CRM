package com.doxa.crm.dto.integration;

public record IntegrationStatusResponse(
        boolean googleConnected,
        boolean googleConfigured,
        boolean telegramConfigured,
        String whatsAppUrl
) {
}
