package com.portal.procucev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ZohoWebhookSignatureUtilTest {

    private ZohoWebhookSignatureUtil util;

    @BeforeEach
    void setUp() {
        util = new ZohoWebhookSignatureUtil();
    }

    @Test
    void testVerifyZohoSignature_NullHeader() {
        assertFalse(util.verifyZohoSignature(null, "body"));
    }

    @Test
    void testVerifyZohoSignature_MissingTimestampOrSignature() {
        assertFalse(util.verifyZohoSignature("t=123", "body"));
        assertFalse(util.verifyZohoSignature("v=sig", "body"));
        assertFalse(util.verifyZohoSignature("invalidpart,t=123", "body"));
    }

    @Test
    void testVerifyZohoSignature_ValidHexKey() {
        ReflectionTestUtils.setField(util, "signingKeyHex", "0x1234567890abcdef1234567890abcdef");
        boolean res = util.verifyZohoSignature("t=12345,v=invalid", "raw_body");
        assertFalse(res);
    }

    @Test
    void testVerifyZohoSignature_ValidBase64Key() {
        ReflectionTestUtils.setField(util, "signingKeyHex", "SGVsbG8gV29ybGQ=");
        boolean res = util.verifyZohoSignature("t=12345,v=invalid", "raw_body");
        assertFalse(res);
    }

    @Test
    void testVerifyZohoSignature_RawKey_ShortLength() {
        ReflectionTestUtils.setField(util, "signingKeyHex", "shortkey");
        boolean res = util.verifyZohoSignature("t=12345,v=invalid", "raw_body");
        assertFalse(res);
    }

    @Test
    void testVerifyZohoSignature_NullSigningKeyHex() {
        ReflectionTestUtils.setField(util, "signingKeyHex", null);
        assertThrows(RuntimeException.class, () -> util.verifyZohoSignature("t=12345,v=invalid", "raw_body"));
    }

    @Test
    void testVerifyZohoSignature_0XPrefix() {
        ReflectionTestUtils.setField(util, "signingKeyHex", "0X1234567890abcdef");
        boolean res = util.verifyZohoSignature("t=12345,v=invalid", "raw_body");
        assertFalse(res);
    }

    @Test
    void testVerifyZohoSignature_MatchingSignature_ReturnsTrue() throws Exception {
        ReflectionTestUtils.setField(util, "signingKeyHex", "0x12345678");
        Method hmacM = ZohoWebhookSignatureUtil.class.getDeclaredMethod("hmacSha256", byte[].class, String.class);
        hmacM.setAccessible(true);
        byte[] key = new byte[]{0x12, 0x34, 0x56, 0x78};
        byte[] hmac = (byte[]) hmacM.invoke(null, key, "100.body");
        Method hexM = ZohoWebhookSignatureUtil.class.getDeclaredMethod("bytesToHex", byte[].class);
        hexM.setAccessible(true);
        String hexSig = (String) hexM.invoke(null, (Object) hmac);

        assertTrue(util.verifyZohoSignature("t=100,v=" + hexSig, "body"));
    }

    @Test
    void testVerifyZohoSignature_ValidSignature_ReturnsTrue() throws Exception {
        String key = "secretkey123";
        ReflectionTestUtils.setField(util, "signingKeyHex", key);

        String timestamp = "1000";
        String body = "{\"event\":\"payment_success\"}";
        String data = timestamp + "." + body;

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String validHexSig = util.verifyZohoSignature("t=" + timestamp + ",v=" + bytesToHex(hmacBytes), body) ? "ok" : "fail";
        assertEquals("ok", validHexSig);

        String validB64Sig = util.verifyZohoSignature("t=" + timestamp + ",v=" + java.util.Base64.getEncoder().encodeToString(hmacBytes), body) ? "ok" : "fail";
        assertEquals("ok", validB64Sig);

        // Valid signature with hex key
        String hexKey = "0x1234567890abcdef1234567890abcdef";
        // Valid Base64 signature in header with Hex key
        ReflectionTestUtils.setField(util, "signingKeyHex", hexKey);
        byte[] hexKeyBytes = new byte[]{0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab, (byte) 0xcd, (byte) 0xef, 0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab, (byte) 0xcd, (byte) 0xef};
        mac.init(new javax.crypto.spec.SecretKeySpec(hexKeyBytes, "HmacSHA256"));
        byte[] hmacHexKey = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + bytesToHex(hmacHexKey), body));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + java.util.Base64.getEncoder().encodeToString(hmacHexKey), body));

        // Valid signature with Base64 key (both Hex and Base64 header signatures)
        String b64Key = "SGVsbG8gV29ybGQhMTIzNDU2Nzg=";
        ReflectionTestUtils.setField(util, "signingKeyHex", b64Key);
        byte[] b64KeyBytes = java.util.Base64.getDecoder().decode(b64Key);
        mac.init(new javax.crypto.spec.SecretKeySpec(b64KeyBytes, "HmacSHA256"));
        byte[] hmacB64Key = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + bytesToHex(hmacB64Key), body));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + java.util.Base64.getEncoder().encodeToString(hmacB64Key), body));

        // Valid signature with Raw key
        String rawKey = "not_valid_base64_or_hex_long_key_value_12345";
        ReflectionTestUtils.setField(util, "signingKeyHex", rawKey);
        byte[] rawKeyBytes = rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        mac.init(new javax.crypto.spec.SecretKeySpec(rawKeyBytes, "HmacSHA256"));
        byte[] hmacRawKey = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + bytesToHex(hmacRawKey), body));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + java.util.Base64.getEncoder().encodeToString(hmacRawKey), body));
        assertTrue(util.verifyZohoSignature("t=" + timestamp + ",v=" + bytesToHex(hmacRawKey).toUpperCase(), body));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Test
    void testVerifyZohoSignature_NullSigningKey_And_KeyPrefixes() {
        ReflectionTestUtils.setField(util, "signingKeyHex", null);
        assertThrows(Exception.class, () -> util.verifyZohoSignature("t=100,v=100,unknown=val,invalidpart", "body"));

        ReflectionTestUtils.setField(util, "signingKeyHex", "0x1234");
        assertFalse(util.verifyZohoSignature("t=100,v=100", "body"));

        assertFalse(util.verifyZohoSignature(null, "body"));
        assertFalse(util.verifyZohoSignature("t=100", "body"));
        assertFalse(util.verifyZohoSignature("v=100", "body"));

        ReflectionTestUtils.setField(util, "signingKeyHex", "0X1234567890abcdef1234567890abcdef");
        assertFalse(util.verifyZohoSignature("other=val,invalidpart,t=100,v=invalid,foo=bar", "body"));

        ReflectionTestUtils.setField(util, "signingKeyHex", "SGVsbG8gV29ybGQhMTIzNDU2Nzg=");
        assertFalse(util.verifyZohoSignature("t=100,v=invalid", "body"));

        ReflectionTestUtils.setField(util, "signingKeyHex", "invalid base64 key with spaces and % sign !");
        assertFalse(util.verifyZohoSignature("t=100,v=invalid", "body"));

        ReflectionTestUtils.setField(util, "signingKeyHex", "0x123");
        assertFalse(util.verifyZohoSignature("t=100,v=invalid", "body"));
    }

    @Test
    void testHexToBytes_And_BytesToHex_And_MessageDigestEquals() throws Exception {
        Method hexToBytesMethod = ZohoWebhookSignatureUtil.class.getDeclaredMethod("hexToBytes", String.class);
        hexToBytesMethod.setAccessible(true);
        byte[] res = (byte[]) hexToBytesMethod.invoke(null, (Object) null);
        assertEquals(0, res.length);

        Method bytesToHexMethod = ZohoWebhookSignatureUtil.class.getDeclaredMethod("bytesToHex", byte[].class);
        bytesToHexMethod.setAccessible(true);
        assertEquals("0102", bytesToHexMethod.invoke(null, (Object) new byte[]{1, 2}));

        Method decodeSigningKeyMethod = ZohoWebhookSignatureUtil.class.getDeclaredMethod("decodeSigningKey", String.class);
        decodeSigningKeyMethod.setAccessible(true);
        byte[] nullKeyBytes = (byte[]) decodeSigningKeyMethod.invoke(null, (Object) null);
        assertEquals(0, nullKeyBytes.length);
    }

    @Test
    void testDecodeSigningKey_HexPrefixes_Base64_And_RawFallback() throws Exception {
        Method decodeSigningKey = ZohoWebhookSignatureUtil.class.getDeclaredMethod("decodeSigningKey", String.class);
        decodeSigningKey.setAccessible(true);

        // lower-case 0x prefix, stripped then decoded as hex
        assertArrayEquals(new byte[]{(byte) 0xaa, (byte) 0xbb},
                (byte[]) decodeSigningKey.invoke(null, "0xaabb"));

        // upper-case 0X prefix takes the second half of the prefix check
        assertArrayEquals(new byte[]{(byte) 0xaa, (byte) 0xbb},
                (byte[]) decodeSigningKey.invoke(null, "0XAABB"));

        // no prefix at all, still valid even-length hex
        assertArrayEquals(new byte[]{(byte) 0xaa, (byte) 0xbb},
                (byte[]) decodeSigningKey.invoke(null, "  aabb  "));

        // hex characters but an odd length, so it falls through to base64
        assertArrayEquals(java.util.Base64.getDecoder().decode("abc"),
                (byte[]) decodeSigningKey.invoke(null, "abc"));

        // not hex at all, decoded as base64
        assertArrayEquals("Hello World".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                (byte[]) decodeSigningKey.invoke(null, "SGVsbG8gV29ybGQ="));

        // neither hex nor base64, falls back to the raw UTF-8 bytes
        assertArrayEquals("not*a*key".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                (byte[]) decodeSigningKey.invoke(null, "not*a*key"));
    }
}
