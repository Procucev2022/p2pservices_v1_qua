package com.portal.procucev.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ZohoWebhookSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(ZohoWebhookSignatureUtil.class);

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

        // detect key format for debugging (without printing the actual key)
        String keyFormat;
        String sKey = signingKeyHex == null ? "" : signingKeyHex.trim();
        String sKeyNoPrefix = sKey.startsWith("0x") || sKey.startsWith("0X") ? sKey.substring(2) : sKey;
        if (sKeyNoPrefix.matches("[0-9a-fA-F]+") && (sKeyNoPrefix.length() % 2 == 0)) {
            keyFormat = "hex";
        } else {
            try {
                Base64.getDecoder().decode(sKey);
                keyFormat = "base64";
            } catch (IllegalArgumentException ignore) {
                keyFormat = "raw";
            }
        }

        byte[] keyBytes = decodeSigningKey(signingKeyHex);
        byte[] hmacRaw = hmacSha256(keyBytes, data);
        String computedHex = bytesToHex(hmacRaw);
        String computedBase64 = Base64.getEncoder().encodeToString(hmacRaw);

        // Normalize to avoid case/whitespace mismatches
        String computedHexNormalized = computedHex.trim().toLowerCase();
        String computedBase64Normalized = computedBase64.trim();
        String headerNormalized = zohoSignature.trim();

        // Debug info - enable DEBUG logging for this class in dev to see values (do not enable in prod)
     //   if (log.isDebugEnabled()) {
            // Compute raw body diagnostics: Base64 and SHA-256 hex so you can compare exact bytes received
            String rawBodyBase64 = Base64.getEncoder().encodeToString(rawBody.getBytes(StandardCharsets.UTF_8));
            String rawBodySha256Hex;
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(rawBody.getBytes(StandardCharsets.UTF_8));
                rawBodySha256Hex = bytesToHex(digest);
            } catch (Exception e) {
                rawBodySha256Hex = "<error>";
            }

            log.info("Zoho webhook signature verification details: timestamp={}, headerSig={}, computedHex={}, computedBase64={}, keyFormat={}, keyLen={}, rawBodySha256={}, rawBodyBase64={}",
                    timestamp, zohoSignature, computedHexNormalized, computedBase64Normalized, keyFormat, keyBytes == null ? 0 : keyBytes.length,
                    rawBodySha256Hex, rawBodyBase64);
      //  }

        // Accept if header equals hex (case-insensitive) or base64 encoding of raw HMAC
        boolean matchesHex = MessageDigest.isEqual(computedHexNormalized.getBytes(StandardCharsets.UTF_8), headerNormalized.toLowerCase().getBytes(StandardCharsets.UTF_8));
        boolean matchesBase64 = MessageDigest.isEqual(computedBase64Normalized.getBytes(StandardCharsets.UTF_8), headerNormalized.getBytes(StandardCharsets.UTF_8));

        return matchesHex || matchesBase64;
    }

    // ---------------- helpers ----------------

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
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

