package com.doxa.crm.util;

public final class WhatsAppLinkBuilder {

    private WhatsAppLinkBuilder() {
    }

    public static String buildUrl(String phoneE164, String message) {
        String digits = phoneE164 == null ? "" : phoneE164.replace("+", "").replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        String base = "https://wa.me/" + digits;
        if (message == null || message.isBlank()) {
            return base;
        }
        return base + "?text=" + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }
}
