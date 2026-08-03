package com.doxa.crm.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class WebhookSignatureVerifier {

    private static final String PREFIX = "sha256=";

    private WebhookSignatureVerifier() {
    }

    public static boolean isValid(String secret, String rawBody, String signatureHeader) {
        if (secret == null || secret.isBlank() || rawBody == null || signatureHeader == null) {
            return false;
        }
        if (!signatureHeader.startsWith(PREFIX)) {
            return false;
        }

        String provided = signatureHeader.substring(PREFIX.length()).trim();
        String expected = hmacSha256Hex(secret, rawBody);
        return constantTimeEquals(provided.toLowerCase(), expected.toLowerCase());
    }

    public static String sign(String secret, String rawBody) {
        return PREFIX + hmacSha256Hex(secret, rawBody);
    }

    private static String hmacSha256Hex(String secret, String rawBody) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to compute webhook signature", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
