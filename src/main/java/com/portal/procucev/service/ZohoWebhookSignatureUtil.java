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

        // Try explicit interpretations of the signing key for debugging and robustness
        String s = signingKeyHex == null ? "" : signingKeyHex.trim();
        String sNoPrefix = s.startsWith("0x") || s.startsWith("0X") ? s.substring(2) : s;

        byte[] keyHexBytes = null;
        if (sNoPrefix.matches("[0-9a-fA-F]+") && (sNoPrefix.length() % 2 == 0)) {
            try { keyHexBytes = hexToBytes(sNoPrefix); } catch (Exception ignored) { keyHexBytes = null; }
        }

        byte[] keyBase64Bytes = null;
        try { keyBase64Bytes = Base64.getDecoder().decode(s); } catch (IllegalArgumentException ignored) { keyBase64Bytes = null; }

        byte[] keyRawBytes = s.getBytes(StandardCharsets.UTF_8);

        byte[] hmacWithHexKey = keyHexBytes == null ? new byte[0] : hmacSha256(keyHexBytes, data);
        byte[] hmacWithBase64Key = keyBase64Bytes == null ? new byte[0] : hmacSha256(keyBase64Bytes, data);
        byte[] hmacWithRawKey = hmacSha256(keyRawBytes, data);

        String computedHexFromHexKey = hmacWithHexKey.length == 0 ? "" : bytesToHex(hmacWithHexKey);
        String computedHexFromBase64Key = hmacWithBase64Key.length == 0 ? "" : bytesToHex(hmacWithBase64Key);
        String computedHexFromRawKey = bytesToHex(hmacWithRawKey);

        String computedBase64FromHexKey = hmacWithHexKey.length == 0 ? "" : Base64.getEncoder().encodeToString(hmacWithHexKey);
        String computedBase64FromBase64Key = hmacWithBase64Key.length == 0 ? "" : Base64.getEncoder().encodeToString(hmacWithBase64Key);
        String computedBase64FromRawKey = Base64.getEncoder().encodeToString(hmacWithRawKey);

        // Normalize
        String headerNormalized = zohoSignature.trim();
        String computedHexFromHexKeyNorm = computedHexFromHexKey.trim().toLowerCase();
        String computedHexFromBase64KeyNorm = computedHexFromBase64Key.trim().toLowerCase();
        String computedHexFromRawKeyNorm = computedHexFromRawKey.trim().toLowerCase();
        String computedBase64FromHexKeyNorm = computedBase64FromHexKey.trim();
        String computedBase64FromBase64KeyNorm = computedBase64FromBase64Key.trim();
        String computedBase64FromRawKeyNorm = computedBase64FromRawKey.trim();

        // Debug info - enable DEBUG logging for this class in dev to see values (do not enable in prod)
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

        // Masked key preview for debugging (do not reveal full key)
        String maskedKeyPreview;
        if (s.length() <= 12) maskedKeyPreview = s;
        else maskedKeyPreview = s.substring(0,6) + "..." + s.substring(s.length()-6);

        // Log all attempts
        log.info("Zoho webhook signature verification details: timestamp={}, headerSig={}, keyPreview={}, keyFormat={}, keyLen={}, rawBodySha256={}, rawBodyBase64={}",
                timestamp, zohoSignature, maskedKeyPreview, keyFormat, (keyHexBytes!=null?keyHexBytes.length:(keyBase64Bytes!=null?keyBase64Bytes.length:keyRawBytes.length)),
                rawBodySha256Hex, rawBodyBase64);

        log.info("Computed signatures (hex) -> fromHexKey={}, fromBase64Key={}, fromRawKey={}", computedHexFromHexKeyNorm, computedHexFromBase64KeyNorm, computedHexFromRawKeyNorm);
        log.info("Computed signatures (base64) -> fromHexKey={}, fromBase64Key={}, fromRawKey={}", computedBase64FromHexKeyNorm, computedBase64FromBase64KeyNorm, computedBase64FromRawKeyNorm);

        // Accept if header equals hex (case-insensitive) or base64 encoding of raw HMAC
        boolean matchesHexFromHex = !computedHexFromHexKeyNorm.isEmpty() && MessageDigest.isEqual(computedHexFromHexKeyNorm.getBytes(StandardCharsets.UTF_8), headerNormalized.toLowerCase().getBytes(StandardCharsets.UTF_8));
        boolean matchesHexFromBase64 = !computedHexFromBase64KeyNorm.isEmpty() && MessageDigest.isEqual(computedHexFromBase64KeyNorm.getBytes(StandardCharsets.UTF_8), headerNormalized.toLowerCase().getBytes(StandardCharsets.UTF_8));
        boolean matchesHexFromRaw = MessageDigest.isEqual(computedHexFromRawKeyNorm.getBytes(StandardCharsets.UTF_8), headerNormalized.toLowerCase().getBytes(StandardCharsets.UTF_8));

        boolean matchesBase64FromHex = !computedBase64FromHexKeyNorm.isEmpty() && MessageDigest.isEqual(computedBase64FromHexKeyNorm.getBytes(StandardCharsets.UTF_8), headerNormalized.getBytes(StandardCharsets.UTF_8));
        boolean matchesBase64FromBase64 = !computedBase64FromBase64KeyNorm.isEmpty() && MessageDigest.isEqual(computedBase64FromBase64KeyNorm.getBytes(StandardCharsets.UTF_8), headerNormalized.getBytes(StandardCharsets.UTF_8));
        boolean matchesBase64FromRaw = MessageDigest.isEqual(computedBase64FromRawKeyNorm.getBytes(StandardCharsets.UTF_8), headerNormalized.getBytes(StandardCharsets.UTF_8));

        boolean anyMatch = matchesHexFromHex || matchesHexFromBase64 || matchesHexFromRaw || matchesBase64FromHex || matchesBase64FromBase64 || matchesBase64FromRaw;

        if (!anyMatch) {
            log.warn("Zoho webhook signature verification failed: no computed signature matched the header");
        } else {
            log.info("Zoho webhook signature verification succeeded using one of the key interpretations");
        }

        return anyMatch;
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

