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

    public boolean verifyZohoSignature(String header, String rawBody) throws Exception {

        // Example: t=1734340423138,v=48f9cb56...
        String[] parts = header.split(",");

        String timestamp = null;
        String zohoSignature = null;

        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if ("t".equals(kv[0])) timestamp = kv[1];
            if ("v".equals(kv[0])) zohoSignature = kv[1];
        }

        if (timestamp == null || zohoSignature == null) return false;

        String data = timestamp + "." + rawBody;

        String computed = hmacSha256Hex(data.getBytes(), signingKeyHex);

        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                zohoSignature.getBytes(StandardCharsets.UTF_8)
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

