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

    public boolean verify(String rawBody, String header) {

        if (header == null || !header.contains("t=") || !header.contains("v=")) {
            return false;
        }

        String[] parts = header.split(",");
        String timestamp = null;
        String receivedSigHex = null;

        for (String p : parts) {
            if (p.startsWith("t=")) timestamp = p.substring(2);
            if (p.startsWith("v=")) receivedSigHex = p.substring(2);
        }

        if (timestamp == null || receivedSigHex == null) return false;

        String payload = timestamp + "." + rawBody;

        String computedHex = hmacSha256Hex(hexToBytes(signingKeyHex), payload);

        return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.UTF_8), receivedSigHex.getBytes(StandardCharsets.UTF_8));
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
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}

