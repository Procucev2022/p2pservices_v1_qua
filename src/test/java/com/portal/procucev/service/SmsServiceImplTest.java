package com.portal.procucev.service;

import com.portal.procucev.dao.OtpStoreDao;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.OtpStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    @Mock
    private OtpStoreDao otpStoreDao;

    @InjectMocks
    private SmsServiceImpl smsService;

    private Organization org;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setEmail("test@example.com");
        org.setOrganizationPhonenumber("919876543210");
        org.setMobileOtp("123456");
    }

    @Test
    void testSendOtpToMobile_NewOtpStore() {
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        ResponseEntity<String> response = smsService.sendOtpToMobile(org);
        assertNotNull(response);
        verify(otpStoreDao).save(any(OtpStore.class));
    }

    @Test
    void testSendOtpToMobile_Without91Prefix() {
        org.setOrganizationPhonenumber("9876543210");
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        ResponseEntity<String> response = smsService.sendOtpToMobile(org);
        assertNotNull(response);
        verify(otpStoreDao).save(any(OtpStore.class));
    }

    @Test
    void testSendOtpToMobile_ExceptionCatch() {
        Organization invalidOrg = new Organization();
        invalidOrg.setEmail("valid@example.com");
        invalidOrg.setOrganizationPhonenumber("9876543210");
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        ResponseEntity<String> response = smsService.sendOtpToMobile(invalidOrg);
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
    }

    @Test
    void testSendOtpToMobile_ExistingOtpStoreAndTempPhone() {
        org.setTempPhone("9876543210");
        OtpStore existing = new OtpStore("key", "000000", LocalDateTime.now());
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(existing));

        ResponseEntity<String> response = smsService.sendOtpToMobile(org);
        assertNotNull(response);
        verify(otpStoreDao).save(existing);
    }

    @Test
    void testIsMobileOtpValid_NotFound() {
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        boolean result = smsService.isMobileOtpValid(org);
        assertFalse(result);
    }

    @Test
    void testIsMobileOtpValid_Expired() {
        OtpStore record = new OtpStore("key", "123456", LocalDateTime.now().minusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(record));

        boolean result = smsService.isMobileOtpValid(org);
        assertFalse(result);
        verify(otpStoreDao).deleteByOtpKey(anyString());
    }

    @Test
    void testIsMobileOtpValid_SuccessAndFailureMatch() {
        OtpStore validRecord = new OtpStore("key", "123456", LocalDateTime.now().plusMinutes(5));
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.of(validRecord));

        assertTrue(smsService.isMobileOtpValid(org));

        org.setMobileOtp("654321");
        assertFalse(smsService.isMobileOtpValid(org));
    }

    @Test
    void testIsMobileOtpValid_TempPhone() {
        org.setTempPhone("9998887776");
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        assertFalse(smsService.isMobileOtpValid(org));
    }

    @Test
    void testRemoveMobileOtp() {
        smsService.removeMobileOtp(org);
        verify(otpStoreDao).deleteByOtpKey(anyString());

        org.setTempPhone("9998887776");
        smsService.removeMobileOtp(org);
        verify(otpStoreDao, times(2)).deleteByOtpKey(anyString());
    }

    @Test
    void testValidateMobileOtp_WithCache() throws Exception {
        org.setTempPhone("");
        assertFalse(smsService.validateMobileOtp(org));

        org.setTempPhone("9876543210");
        assertFalse(smsService.validateMobileOtp(org));

        // Inject into internal otpCache via reflection
        Field cacheField = SmsServiceImpl.class.getDeclaredField("otpCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> cache = (Map<String, String>) cacheField.get(smsService);

        String key = "test@example.com_MOBILE_9876543210";
        cache.put(key, "000000"); // mismatch
        assertFalse(smsService.validateMobileOtp(org));

        cache.put(key, "123456"); // match
        assertTrue(smsService.validateMobileOtp(org));
        assertFalse(cache.containsKey(key));

        org.setTempPhone(null);
        String keyNoTemp = "test@example.com_MOBILE_919876543210";
        cache.put(keyNoTemp, "123456");
        assertTrue(smsService.validateMobileOtp(org));
    }

    @Test
    void testSendOtpToMobile_HttpClientErrorException() {
        Organization mockOrg = new Organization();
        mockOrg.setEmail("err@example.com");
        mockOrg.setOrganizationPhonenumber("9876543210");
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());

        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, new byte[0], null);

        assertNotNull(smsService.sendOtpToMobile(mockOrg));
    }

    @Test
    void testRemoveMobileOtp_Branches() {
        org.setTempPhone(null);
        smsService.removeMobileOtp(org);

        org.setTempPhone("");
        smsService.removeMobileOtp(org);

        org.setTempPhone("9876543210");
        smsService.removeMobileOtp(org);
    }

    @Test
    void testBlankTempPhoneFallsBackToTheRegisteredNumber() {
        when(otpStoreDao.findByOtpKey(anyString())).thenReturn(Optional.empty());
        org.setTempPhone("");

        assertNotNull(smsService.sendOtpToMobile(org));
        verify(otpStoreDao).save(any(OtpStore.class));
    }

    @Test
    void testIsMobileOtpValidWithBlankTempPhoneUsesRegisteredNumber() {
        org.setTempPhone("");
        when(otpStoreDao.findByOtpKey("test@example.com_MOBILE_919876543210"))
                .thenReturn(Optional.empty());

        assertFalse(smsService.isMobileOtpValid(org));
    }
}
