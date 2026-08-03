package com.doxa.crm.util;

import java.util.regex.Pattern;

public final class PhoneNormalizer {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D+");

    private PhoneNormalizer() {
    }

    /**
     * Normalizes a phone number to E.164 when possible (defaults US +1 for 10-digit numbers).
     */
    public static String toE164(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String trimmed = rawPhone.trim();
        if (trimmed.startsWith("+")) {
            String digits = NON_DIGIT.matcher(trimmed.substring(1)).replaceAll("");
            return digits.isEmpty() ? null : "+" + digits;
        }

        String digits = NON_DIGIT.matcher(trimmed).replaceAll("");
        if (digits.length() == 10) {
            return "+1" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }
        return digits.isEmpty() ? null : "+" + digits;
    }

    public static String dedupeKey(String email, String phoneE164) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedPhone = phoneE164 == null ? "" : phoneE164;
        if (!normalizedEmail.isBlank()) {
            return "email:" + normalizedEmail;
        }
        if (!normalizedPhone.isBlank()) {
            return "phone:" + normalizedPhone;
        }
        return "unknown:" + java.util.UUID.randomUUID();
    }
}
