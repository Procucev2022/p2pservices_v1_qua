package com.portal.procucev.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class ZohoWebhookSignatureUtil {

    @Value("${zoho.webhook.signing-key}")
    private String signingKeyHex;

    // header example: t=1734340423138,v=48f9cb56...
    public boolean verifyZohoSignature(String header, String rawBody) {

        if (header == null) return false;

        String[] parts = header.split(",");

        String timestamp = null;
        String zohoSignature = null;

        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            if ("t".equals(key)) timestamp = val;
            if ("v".equals(key)) zohoSignature = val;
        }

        if (timestamp == null || zohoSignature == null) return false;

        String data = timestamp + "." + rawBody;

        byte[] keyBytes = decodeSigningKey(signingKeyHex);
        String computedHex = hmacSha256Hex(keyBytes, data);

        // Normalize to lower-case and trim to avoid case/whitespace mismatches
        String computedNormalized = computedHex.trim().toLowerCase();
        String headerNormalized = zohoSignature.trim().toLowerCase();

        return MessageDigest.isEqual(
                computedNormalized.getBytes(StandardCharsets.UTF_8),
                headerNormalized.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ---------------- helpers ----------------

    private static String hmacSha256Hex(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null) return new byte[0];
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    private static byte[] decodeSigningKey(String key) {
        if (key == null) return new byte[0];
        String s = key.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);

        // If it looks like hex (only hex chars and even length), decode hex
        if (s.matches("[0-9a-fA-F]+") && (s.length() % 2 == 0)) {
            return hexToBytes(s);
        }

        // Try base64
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException e) {
            // fallback: return raw bytes of the string
            return s.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

