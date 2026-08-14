package com.portal.procucev.controller;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.User;
import com.portal.procucev.service.ExcelReader;
import com.portal.procucev.service.GMTService;
import com.portal.procucev.service.SelfRegistrationService;
import com.portal.procucev.service.SmsService;
import com.portal.procucev.service.UserService;
import com.portal.procucev.utils.ClientRegistrationStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartialVendorControllerTest {

    @Mock
    private SelfRegistrationService regService;
    @Mock
    private SmsService smsService;
    @Mock
    private GMTService gmtService;
    @Mock
    private UserService userServices;
    @Mock
    private ExcelReader excelReader;

    @InjectMocks
    private PartialVendorController controller;

    private Organization org;
    private User user;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setEmail("test@org.com");
        org.setOrganizationPhonenumber("9876543210");

        user = new User();
        user.setUsername("testuser");
    }

    @Test
    void testVendorRegistration_Success() throws Exception {
        when(regService.vendorRegistration(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.vendorRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testVendorRegistration_AppException() throws Exception {
        when(regService.vendorRegistration(any())).thenThrow(new AppException(400, "App error", "type", "fail"));
        ResponseEntity<?> resp = controller.vendorRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testVendorRegistration_GenericException() throws Exception {
        when(regService.vendorRegistration(any())).thenThrow(new RuntimeException("generic err"));
        ResponseEntity<?> resp = controller.vendorRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testSendOtp_SuccessAndFailure() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(regService.generateOtp(any(), any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.sendOtp(org, req);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(regService.generateOtp(any(), any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.sendOtp(org, req);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
    }

    @Test
    void testValidateOtp_SuccessAndFailure() {
        when(regService.validateEmailOtp(any())).thenReturn(true);
        ResponseEntity<?> resp1 = controller.validateOtp(org);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());
        verify(regService).removeEmailOtp(org);

        when(regService.validateEmailOtp(any())).thenReturn(false);
        ResponseEntity<?> resp2 = controller.validateOtp(org);
        assertEquals(HttpStatus.UNAUTHORIZED, resp2.getStatusCode());
    }

    @Test
    void testValidateClient() {
        when(regService.validateClient(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.validateClient(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testClientRegistration_NewClient() throws Exception {
        when(regService.selfclientRegistrationData(any())).thenReturn(ClientRegistrationStatus.NEW_CLIENT);
        ResponseEntity<MessageResponse> resp = controller.clientRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testClientRegistration_ExistingClient() throws Exception {
        when(regService.selfclientRegistrationData(any())).thenReturn(ClientRegistrationStatus.EXISTING_CLIENT);
        ResponseEntity<MessageResponse> resp = controller.clientRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testClientRegistration_AppException() throws Exception {
        when(regService.selfclientRegistrationData(any())).thenThrow(new AppException(400, "error", "type", "fail"));
        ResponseEntity<MessageResponse> resp = controller.clientRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testClientRegistration_GenericException() throws Exception {
        when(regService.selfclientRegistrationData(any())).thenThrow(new RuntimeException("generic err"));
        ResponseEntity<MessageResponse> resp = controller.clientRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testGetClientByPan() throws Exception {
        when(regService.getClientByPan(any())).thenReturn(org);
        ResponseEntity<?> resp = controller.getClientByPan(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testValidateClientDetails_UserExists_And_NewUser() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(regService.userExistsByEmailAndPhone(anyString(), anyString())).thenReturn(true);
        ResponseEntity<Map<String, Object>> resp1 = controller.validateClientDetails(org, req);
        assertEquals("error", resp1.getBody().get("status"));

        when(regService.userExistsByEmailAndPhone(anyString(), anyString())).thenReturn(false);
        when(regService.generateOtp(any(), any())).thenReturn(true);
        when(smsService.sendOtpToMobile(any())).thenReturn(ResponseEntity.ok("SMS Sent"));
        ResponseEntity<Map<String, Object>> resp2 = controller.validateClientDetails(org, req);
        assertEquals(true, resp2.getBody().get("otpSentToEmail"));
    }

    @Test
    void testValidateAllOtps_BothValid_And_Invalid() {
        when(regService.isEmailOtpValid(any())).thenReturn(true);
        when(smsService.isMobileOtpValid(any())).thenReturn(true);
        ResponseEntity<Map<String, Object>> resp1 = controller.validateAllOtps(org);
        assertEquals("success", resp1.getBody().get("status"));
        verify(regService).removeEmailOtp(org);
        verify(smsService).removeMobileOtp(org);

        when(regService.isEmailOtpValid(any())).thenReturn(false);
        when(smsService.isMobileOtpValid(any())).thenReturn(false);
        ResponseEntity<Map<String, Object>> resp2 = controller.validateAllOtps(org);
        assertEquals("failure", resp2.getBody().get("status"));
    }

    @Test
    void testGetPincodeDetails() {
        when(regService.getPincodeDetails(anyString())).thenReturn(Collections.emptyList());
        ResponseEntity<List<Map<String, Object>>> resp = controller.getPincodeDetails("560001");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testUploadCsv() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        when(regService.importFromCsv(any())).thenReturn(5);
        ResponseEntity<String> resp = controller.uploadCsv(file);
        assertTrue(resp.getBody().contains("5"));
    }

    @Test
    void testGetPincodeData_Found_And_Null() throws Exception {
        PincodeData data = new PincodeData();
        when(regService.getCityByPincode(any())).thenReturn(data);
        ResponseEntity<?> resp1 = controller.getPincodeData(data);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(regService.getCityByPincode(any())).thenReturn(null);
        assertThrows(AppException.class, () -> controller.getPincodeData(data));
    }

    @Test
    void testValidateClientDetails_UserExists_And_SmsNon2xx() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(regService.userExistsByEmailAndPhone(anyString(), anyString())).thenReturn(true);
        ResponseEntity<Map<String, Object>> respExists = controller.validateClientDetails(org, req);
        assertEquals("error", respExists.getBody().get("status"));

        when(regService.userExistsByEmailAndPhone(anyString(), anyString())).thenReturn(false);
        when(regService.generateOtp(any(), any())).thenReturn(true);
        when(smsService.sendOtpToMobile(any())).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("SMS Failed"));
        ResponseEntity<Map<String, Object>> resp = controller.validateClientDetails(org, req);
        assertEquals(false, resp.getBody().get("otpSentToMobile"));
    }

    @Test
    void testValidateOtp_Unauthorized() {
        when(regService.validateEmailOtp(any())).thenReturn(false);
        ResponseEntity<?> resp = controller.validateOtp(org);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void testUploadCsv_Exception() throws Exception {
        MockMultipartFile badFile = mock(MockMultipartFile.class);
        when(badFile.getOriginalFilename()).thenReturn("test.csv");
        when(regService.importFromCsv(any())).thenThrow(new RuntimeException("csv err"));
        assertThrows(RuntimeException.class, () -> controller.uploadCsv(badFile));
    }

    @Test
    void testGetUsersByPhone_NullUsers() {
        when(regService.getUsersByPhoneNumber("123")).thenReturn(null);
        ResponseEntity<?> resp = controller.getUsersByPhone("123");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testBuyerRegistration_Success() throws Exception {
        Map<String, Object> mapSuccess = new HashMap<>();
        mapSuccess.put("confirmationFlag", true);
        when(regService.selfclientRegistrationDataByApp(any())).thenReturn(mapSuccess);
        ResponseEntity<MessageResponse> resp = controller.buyerRegistration(org);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testBuyerRegistration_AppException() throws Exception {
        when(regService.selfclientRegistrationDataByApp(any())).thenThrow(new AppException(400, "app err", "type", "fail"));
        ResponseEntity<MessageResponse> resp = controller.buyerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void testBuyerRegistration_GenericException() throws Exception {
        when(regService.selfclientRegistrationDataByApp(any())).thenThrow(new RuntimeException("generic err"));
        ResponseEntity<MessageResponse> resp = controller.buyerRegistration(org);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void testForgotPassword() {
        when(userServices.forgotPassword(any())).thenReturn(true);
        ResponseEntity<?> resp = controller.forgotPassword(user);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testBuyerRegistration_FailureFlag() throws Exception {
        Map<String, Object> mapFail = new HashMap<>();
        mapFail.put("confirmationFlag", false);
        mapFail.put("error", "Failed registration");
        when(regService.selfclientRegistrationDataByApp(any())).thenReturn(mapFail);
        ResponseEntity<MessageResponse> resp = controller.buyerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void testSellerRegistration_Success() throws Exception {
        Map<String, Object> mapSuccess = new HashMap<>();
        mapSuccess.put("confirmationFlag", true);
        when(regService.sellerRegistration(any())).thenReturn(mapSuccess);
        ResponseEntity<MessageResponse> resp4 = controller.sellerRegistration(org);
        assertEquals(HttpStatus.OK, resp4.getStatusCode());
    }

    @Test
    void testClientRegistration_AllStatuses() throws Exception {
        when(regService.selfclientRegistrationData(any()))
                .thenReturn(com.portal.procucev.utils.ClientRegistrationStatus.NEW_CLIENT)
                .thenReturn(com.portal.procucev.utils.ClientRegistrationStatus.EXISTING_CLIENT)
                .thenThrow(new AppException(400, "app err", "type", "fail"))
                .thenThrow(new RuntimeException("sys err"));

        assertEquals(HttpStatus.OK, controller.clientRegistration(org).getStatusCode());
        assertEquals(HttpStatus.OK, controller.clientRegistration(org).getStatusCode());
        assertEquals(HttpStatus.OK, controller.clientRegistration(org).getStatusCode());
        assertEquals(HttpStatus.OK, controller.clientRegistration(org).getStatusCode());
    }

    @Test
    void testBuyerAndSellerRegistration_NullErrorMsgInMap() throws Exception {
        Map<String, Object> respMapFail = new HashMap<>();
        respMapFail.put("confirmationFlag", false); // no error key

        when(regService.selfclientRegistrationDataByApp(any())).thenReturn(respMapFail);
        ResponseEntity<MessageResponse> resp1 = controller.buyerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp1.getStatusCode());

        when(regService.sellerRegistration(any())).thenReturn(respMapFail);
        ResponseEntity<MessageResponse> resp2 = controller.sellerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());
    }

    @Test
    void testSellerRegistration_GenericException() throws Exception {
        when(regService.sellerRegistration(any())).thenThrow(new RuntimeException("generic err"));
        ResponseEntity<MessageResponse> resp = controller.sellerRegistration(org);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

    @Test
    void testSellerRegistration_FailureFlag() throws Exception {
        Map<String, Object> mapFail = new HashMap<>();
        mapFail.put("confirmationFlag", false);
        mapFail.put("error", "Seller reg failed");
        when(regService.sellerRegistration(any())).thenReturn(mapFail);
        ResponseEntity<MessageResponse> resp = controller.sellerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void testSellerRegistration_AppException() throws Exception {
        when(regService.sellerRegistration(any())).thenThrow(new AppException(400, "app err", "type", "fail"));
        ResponseEntity<MessageResponse> resp = controller.sellerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }

    @Test
    void testUploadExcel1_EmptyAndValidAndException() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        ResponseEntity<String> resp1 = controller.uploadExcel1(emptyFile);
        assertEquals(HttpStatus.BAD_REQUEST, resp1.getStatusCode());

        MockMultipartFile validFile = new MockMultipartFile("file", "a.xlsx", "text/plain", new byte[]{1, 2});
        ResponseEntity<String> resp2 = controller.uploadExcel1(validFile);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
        verify(excelReader).uploadExcelToDB(any(InputStream.class));

        doThrow(new RuntimeException("read err")).when(excelReader).uploadExcelToDB(any(InputStream.class));
        ResponseEntity<String> resp3 = controller.uploadExcel1(validFile);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp3.getStatusCode());
    }

    @Test
    void testUploadExcel1_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        ResponseEntity<String> resp = controller.uploadExcel1(emptyFile);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void testUploadBuyerDetails_SuccessAndException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.xlsx", "text/plain", new byte[]{1, 2});
        ResponseEntity<String> resp1 = controller.uploadExcel(file);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        doThrow(new RuntimeException("err")).when(regService).registerFromExcel(any());
        ResponseEntity<String> resp2 = controller.uploadExcel(file);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp2.getStatusCode());
    }

    @Test
    void testUploadCategoriesExcel_SuccessAndException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.xlsx", "text/plain", new byte[]{1, 2});
        ResponseEntity<String> resp1 = controller.uploadCategoriesExcel(file);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        doThrow(new RuntimeException("err")).when(regService).importCategoriesFromExcel(any());
        ResponseEntity<String> resp2 = controller.uploadCategoriesExcel(file);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp2.getStatusCode());
    }

    @Test
    void testUpdateVendorClasses() {
        ResponseEntity<?> resp = controller.updateVendorClasses();
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(gmtService).updateVendorClasses();
    }

    @Test
    void testUploadSellerDetails_SuccessAndException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.xlsx", "text/plain", new byte[]{1, 2});
        when(regService.SellerregisterFromExcel(any())).thenReturn(Collections.emptyMap());
        ResponseEntity<String> resp1 = controller.uploadSellerDetails(file);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        doThrow(new RuntimeException("err")).when(regService).SellerregisterFromExcel(any());
        ResponseEntity<String> resp2 = controller.uploadSellerDetails(file);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp2.getStatusCode());
    }

    @Test
    void testBuyerRegistration_Branches() throws Exception {
        Map<String, Object> respMapSuccess = new HashMap<>();
        respMapSuccess.put("confirmationFlag", true);

        Map<String, Object> respMapFail = new HashMap<>();
        respMapFail.put("confirmationFlag", false);
        respMapFail.put("error", "err msg");

        when(regService.selfclientRegistrationDataByApp(any()))
                .thenReturn(respMapSuccess)
                .thenReturn(respMapFail)
                .thenThrow(new AppException(400, "app err", "type", "fail"))
                .thenThrow(new RuntimeException("sys err"));

        ResponseEntity<MessageResponse> resp1 = controller.buyerRegistration(org);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        ResponseEntity<MessageResponse> resp2 = controller.buyerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());

        ResponseEntity<MessageResponse> resp3 = controller.buyerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp3.getStatusCode());

        ResponseEntity<MessageResponse> resp4 = controller.buyerRegistration(org);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp4.getStatusCode());
    }

    @Test
    void testSellerRegistration_Branches() throws Exception {
        Map<String, Object> respMapSuccess = new HashMap<>();
        respMapSuccess.put("confirmationFlag", true);

        Map<String, Object> respMapFail = new HashMap<>();
        respMapFail.put("confirmationFlag", false);
        respMapFail.put("error", "err msg");

        when(regService.sellerRegistration(any()))
                .thenReturn(respMapSuccess)
                .thenReturn(respMapFail)
                .thenThrow(new AppException(400, "app err", "type", "fail"))
                .thenThrow(new RuntimeException("sys err"));

        ResponseEntity<MessageResponse> resp1 = controller.sellerRegistration(org);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        ResponseEntity<MessageResponse> resp2 = controller.sellerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());

        ResponseEntity<MessageResponse> resp3 = controller.sellerRegistration(org);
        assertEquals(HttpStatus.CONFLICT, resp3.getStatusCode());

        ResponseEntity<MessageResponse> resp4 = controller.sellerRegistration(org);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp4.getStatusCode());
    }

    @Test
    void testValidateAllOtps_Branches() {
        when(regService.userExistsByEmailAndPhone(any(), any())).thenReturn(false);
        when(regService.generateOtp(any(), any())).thenReturn(true);
        when(smsService.sendOtpToMobile(any())).thenReturn(ResponseEntity.ok("ok"));
        assertEquals(HttpStatus.OK, controller.validateClientDetails(org, mock(HttpServletRequest.class)).getStatusCode());

        when(smsService.sendOtpToMobile(any())).thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("err"));
        assertEquals(HttpStatus.OK, controller.validateClientDetails(org, mock(HttpServletRequest.class)).getStatusCode());

        when(regService.isEmailOtpValid(any())).thenReturn(true);
        when(smsService.isMobileOtpValid(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.validateAllOtps(org).getStatusCode());

        when(regService.isEmailOtpValid(any())).thenReturn(false);
        when(smsService.isMobileOtpValid(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.validateAllOtps(org).getStatusCode());

        when(regService.isEmailOtpValid(any())).thenReturn(true);
        when(smsService.isMobileOtpValid(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.validateAllOtps(org).getStatusCode());
    }

    @Test
    void testGetPincodeData_Null() {
        when(regService.getCityByPincode(any())).thenReturn(null);
        assertThrows(AppException.class, () -> controller.getPincodeData(new PincodeData()));
    }

    @Test
    void testUploadExcel_EmptyAndException() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/csv", new byte[0]);
        assertEquals(HttpStatus.BAD_REQUEST, controller.uploadExcel1(emptyFile).getStatusCode());

        MultipartFile errFile = mock(MultipartFile.class);
        when(errFile.isEmpty()).thenReturn(false);
        when(errFile.getInputStream()).thenThrow(new IOException("err"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.uploadExcel1(errFile).getStatusCode());
    }

    @Test
    void testUploadDetails_Exceptions() throws Exception {
        MultipartFile errFile = mock(MultipartFile.class);
        when(errFile.getInputStream()).thenThrow(new IOException("err"));
        doThrow(new RuntimeException("err")).when(regService).registerFromExcel(any());
        doThrow(new RuntimeException("err")).when(regService).importCategoriesFromExcel(any());
        doThrow(new RuntimeException("err")).when(regService).SellerregisterFromExcel(any());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.uploadExcel(errFile).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.uploadCategoriesExcel(errFile).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.uploadSellerDetails(errFile).getStatusCode());
    }

    @Test
    void testForgotPassword_And_UpdateVendorClasses() {
        when(userServices.forgotPassword(any())).thenReturn(true);
        assertEquals(HttpStatus.OK, controller.forgotPassword(new User()).getStatusCode());

        when(userServices.forgotPassword(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.forgotPassword(new User()).getStatusCode());

        assertEquals(HttpStatus.OK, controller.updateVendorClasses().getStatusCode());
    }
    @Test
    void testGetUsersByPhone_Null_And_ErrorMsgInRegistrationMap() throws Exception {
        when(regService.getUsersByPhoneNumber("000")).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.getUsersByPhone("000").getStatusCode());

        Map<String, Object> mapFailWithError = new HashMap<>();
        mapFailWithError.put("confirmationFlag", false);
        mapFailWithError.put("error", "custom error msg");

        when(regService.selfclientRegistrationDataByApp(any())).thenReturn(mapFailWithError);
        assertEquals(HttpStatus.CONFLICT, controller.buyerRegistration(org).getStatusCode());

        when(regService.sellerRegistration(any())).thenReturn(mapFailWithError);
        assertEquals(HttpStatus.CONFLICT, controller.sellerRegistration(org).getStatusCode());
    }

    @Test
    void testExactMissedBranches() throws Exception {
        when(regService.vendorRegistration(any())).thenReturn(false);
        assertEquals(HttpStatus.OK, controller.vendorRegistration(org).getStatusCode());

        when(regService.getUsersByPhoneNumber("empty")).thenReturn(Collections.emptyList());
        assertEquals(HttpStatus.OK, controller.getUsersByPhone("empty").getStatusCode());

        User foundUser = new User();
        foundUser.setUsername("found@test.com");
        when(regService.getUsersByPhoneNumber("found")).thenReturn(Collections.singletonList(foundUser));
        ResponseEntity<?> foundResponse = controller.getUsersByPhone("found");
        assertEquals(HttpStatus.OK, foundResponse.getStatusCode());
        assertInstanceOf(MessageResponse.class, foundResponse.getBody());

        when(regService.isEmailOtpValid(org)).thenReturn(false);
        when(smsService.isMobileOtpValid(org)).thenReturn(true);
        ResponseEntity<Map<String, Object>> otpResponse = controller.validateAllOtps(org);
        assertEquals("failure", otpResponse.getBody().get("status"));
        assertEquals(Collections.singletonList("email"), otpResponse.getBody().get("otpType"));
    }
}
