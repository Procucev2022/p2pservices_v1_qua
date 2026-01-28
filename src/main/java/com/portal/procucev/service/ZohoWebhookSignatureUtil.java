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

    public boolean verify(String payload, String receivedSignature) {

        if (receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }

        String computed = computeSignature(payload);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }


//    public boolean verify(String payload, String receivedSignature) {
//        String computed = computeSignature(payload);
//        return MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8), receivedSignature.getBytes(StandardCharsets.UTF_8));
//    }

    public String computeSignature(String payload) {
        try {
            byte[] keyBytes = hexToBytes(signingKeyHex);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);

        } catch (Exception e) {
            throw new RuntimeException("Signature computation failed", e);
        }
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
