package com.portal.procucev.controller;

import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.customexception.RfqDocumentSizeExceededException;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.User;
import com.portal.procucev.service.AutomaticRfqService;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutomaticRfqCreationControllerTest {

    @Mock
    private AutomaticRfqService autoRfqService;
    @Mock
    private PincodeDao pincodeDao;
    @Mock
    private UserDao userDao;
    @Mock
    private OrgDao orgDao;

    @InjectMocks
    private AutomaticRfqCreationController controller;

    private User user;
    private Organization org;
    private Rfq rfq;

    @BeforeEach
    void setUp() {
        org = new Organization();
        org.setCity("CityA");
        org.setState("StateA");
        org.setZipCode("560001");

        user = new User();
        user.setId("U1");
        user.setOrg(org);

        rfq = new Rfq();
        rfq.setUser("U1");
    }

    @Test
    void testRaiseRfqAuto_UserNotFound() {
        when(userDao.findById("U1")).thenReturn(Optional.empty());
        ResponseEntity<?> resp = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    /**
     * Delivery-location resolution now lives in the service so the email pipeline gets the same
     * pincode and organisation fallbacks. The endpoint's job is to delegate; the resolution rules
     * themselves are covered against the real implementation in SharedRfqCreationPipelineTest.
     */
    @Test
    void testRaiseRfqAuto_DelegatesDeliveryLocationResolutionToService() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(true);

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        verify(autoRfqService).resolveDeliveryLocation(rfq, user);
        verify(autoRfqService).raiseRfq(rfq);
    }

    @Test
    void testRaiseRfqAuto_OversizedDocumentReturnsPayloadTooLarge() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenThrow(
                new RfqDocumentSizeExceededException("drawing.jpg", 30_000_000L, 26214400L));

        ResponseEntity<?> resp = controller.raiseRfqAuto(rfq);

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, resp.getStatusCode());
        assertNotNull(resp.getBody());
        MessageResponse body = assertInstanceOf(MessageResponse.class, resp.getBody());
        assertTrue(body.getMessage().contains("drawing.jpg"));
        assertTrue(body.getMessage().contains("exceeds"));
    }

    @Test
    void testRaiseRfqAuto_EmptyOrNullDeliveryList() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(false);

        rfq.setClientdeliverylocationrfq(Collections.emptyList());
        ResponseEntity<?> resp = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        
        rfq.setClientdeliverylocationrfq(null);
        ResponseEntity<?> resp2 = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testRaiseRfqAuto_AllLocationParamsPresent() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(true);

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("CityX");
        loc.setState("StateX");
        loc.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        ResponseEntity<?> resp = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testRaiseRfqAuto_OnlyPincode_FoundAndNotFound() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(true);

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity(null);
        loc.setState(null);
        loc.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        when(pincodeDao.findByPincode("560001")).thenReturn(new PincodeData("560001", "CityP", "StateP"));
        ResponseEntity<?> resp1 = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(pincodeDao.findByPincode("560001")).thenReturn(null);
        ResponseEntity<?> resp2 = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testRaiseRfqAuto_OnlyCity_FoundAndNotFound() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(true);

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("CityX");
        loc.setState(null);
        loc.setPincode(null);
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        when(pincodeDao.findByCityIgnoreCase("CityX")).thenReturn(new PincodeData("560001", "CityX", "StateX"));
        ResponseEntity<?> resp1 = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp1.getStatusCode());

        when(pincodeDao.findByCityIgnoreCase("CityX")).thenReturn(null);
        ResponseEntity<?> resp2 = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp2.getStatusCode());
    }

    @Test
    void testRaiseRfqAuto_NoLocation_And_FailureResponse() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(false);

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("   ");
        loc.setState("   ");
        loc.setPincode("   ");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        ResponseEntity<?> resp = controller.raiseRfqAuto(rfq);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void testRaiseRfqAuto_ShortCircuitConditions() {
        when(userDao.findById("U1")).thenReturn(Optional.of(user));
        when(autoRfqService.raiseRfq(any())).thenReturn(true);
        when(pincodeDao.findByPincode(anyString())).thenReturn(new PincodeData("560001", "CityP", "StateP"));
        when(pincodeDao.findByCityIgnoreCase(anyString())).thenReturn(new PincodeData("560001", "CityX", "StateX"));

        // 1. City valid, State valid, Pincode null
        ClientDeliveryLocationRfq l1 = new ClientDeliveryLocationRfq();
        l1.setCity("CityX"); l1.setState("StateX"); l1.setPincode(null);
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l1));
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        // 2. City valid, State valid, Pincode empty
        ClientDeliveryLocationRfq l2 = new ClientDeliveryLocationRfq();
        l2.setCity("CityX"); l2.setState("StateX"); l2.setPincode("   ");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l2));
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        // 3. City valid, State null, Pincode valid
        ClientDeliveryLocationRfq l3 = new ClientDeliveryLocationRfq();
        l3.setCity("CityX"); l3.setState(null); l3.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l3));
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        // 4. City valid, State empty, Pincode valid
        ClientDeliveryLocationRfq l4 = new ClientDeliveryLocationRfq();
        l4.setCity("CityX"); l4.setState("   "); l4.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l4));
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        // 5. City null, State valid, Pincode valid
        ClientDeliveryLocationRfq l5 = new ClientDeliveryLocationRfq();
        l5.setCity(null); l5.setState("StateX"); l5.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l5));
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        // 7. Only pincode provided, pincodeDao returns null
        ClientDeliveryLocationRfq l7 = new ClientDeliveryLocationRfq();
        l7.setCity(null); l7.setState(null); l7.setPincode("999999");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l7));
        when(pincodeDao.findByPincode("999999")).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());

        // 8. Only city provided, pincodeDao returns null
        ClientDeliveryLocationRfq l8 = new ClientDeliveryLocationRfq();
        l8.setCity("UnknownCity"); l8.setState(null); l8.setPincode(null);
        rfq.setClientdeliverylocationrfq(Collections.singletonList(l8));
        when(pincodeDao.findByCityIgnoreCase("UnknownCity")).thenReturn(null);
        assertEquals(HttpStatus.OK, controller.raiseRfqAuto(rfq).getStatusCode());
    }

    @Test
    void testValidateEmail() {
        when(autoRfqService.validateEmail(anyString())).thenReturn(new HashMap<>());
        ResponseEntity<?> resp = controller.validateEmail("test@example.com");
        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }
}
