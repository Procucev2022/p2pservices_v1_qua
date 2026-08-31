package com.portal.procucev.service;

import com.portal.procucev.customexception.RfqDocumentSizeExceededException;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.*;
import com.portal.procucev.rfq.repository.RFQRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutomaticRfqServiceImplTest {

    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private GmtItemsDao gmtItemsDao;
    @Mock
    private RfqDao rfqDao;
    @Mock
    private UserDao userDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private PincodeDao pincodeDao;
    @Mock
    private RFQRepository rfqRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AutomaticRfqServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(1);
    }

    @Test
    void testGetMaxDocumentBytes() {
        assertEquals(26214400L, service.getMaxDocumentBytes());
    }

    @Test
    void testRaiseRfq_Success() {
        Rfq rfq = new Rfq();
        List<RfqItem> items = new ArrayList<>();
        RfqItem item = new RfqItem();
        item.setId("ITEM1");
        item.setBrand("Brand");
        item.setDescription("Desc");
        item.setQuantity(10.0);
        items.add(item);
        rfq.setRfqItem(items);

        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        boolean result = service.raiseRfq(rfq);
        assertTrue(result);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(rfqDao, gmtItemsDao);
        inOrder.verify(rfqDao).saveAndFlush(rfq);
        inOrder.verify(gmtItemsDao).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void testRaiseRfq_LinksEveryGmtItemToItsRfqItem() {
        Rfq rfq = new Rfq();
        List<RfqItem> items = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            RfqItem item = new RfqItem();
            item.setId("ITEM" + i);
            item.setDescription("Item " + i);
            item.setQuantity(i * 100.0);
            items.add(item);
        }
        rfq.setRfqItem(items);
        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        assertTrue(service.raiseRfq(rfq));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<com.portal.procucev.model.GmtItems>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(gmtItemsDao).saveAll(captor.capture());

        List<com.portal.procucev.model.GmtItems> saved = captor.getValue();
        assertEquals(4, saved.size(), "every line item must get its own GMT row");
        for (int i = 0; i < saved.size(); i++) {
            assertEquals("ITEM" + (i + 1), saved.get(i).getRfqItemId(),
                    "each GMT row must be linked to the RFQ item it describes");
        }
    }

    @Test
    void testRaiseRfq_DefaultQuantityAndUom() {
        Rfq rfq = new Rfq();
        List<RfqItem> items = new ArrayList<>();
        RfqItem item = new RfqItem();
        item.setQuantity(0.0);
        item.setUnitofMeasures(null);
        items.add(item);
        rfq.setRfqItem(items);

        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        assertTrue(service.raiseRfq(rfq));
        assertEquals(1.0, item.getQuantity());
        assertEquals("Nos", item.getUnitofMeasures());
    }

    @Test
    void testRaiseRfq_Exception() {
        Rfq rfq = new Rfq();
        rfq.setRfqItem(new ArrayList<>());
        when(masterStatusDao.findByStatus(anyString())).thenThrow(new DataIntegrityViolationException("Error"));

        boolean result = service.raiseRfq(rfq);
        assertFalse(result);
    }

    @Test
    void testResolveDeliveryLocation_NullOrEmptyRfq() {
        assertDoesNotThrow(() -> service.resolveDeliveryLocation(null, null));

        Rfq rfq = new Rfq();
        User user = new User();
        Organization org = new Organization();
        org.setCity("Hyd");
        org.setState("TS");
        org.setZipCode("500001");
        user.setOrg(org);

        service.resolveDeliveryLocation(rfq, user);
        assertNotNull(rfq.getClientdeliverylocationrfq());
        assertEquals("Hyd", rfq.getClientdeliverylocationrfq().get(0).getCity());
    }

    @Test
    void testResolveDeliveryLocation_CompleteAddress() {
        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("Bangalore");
        loc.setState("Karnataka");
        loc.setPincode("560001");
        rfq.setClientdeliverylocationrfq(List.of(loc));

        service.resolveDeliveryLocation(rfq, null);
        assertEquals("Bangalore", loc.getCity());
        assertEquals("Karnataka", loc.getState());
        assertEquals("560001", loc.getPincode());
    }

    @Test
    void testResolveDeliveryLocation_PincodeLookup() {
        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setPincode("560001");
        rfq.setClientdeliverylocationrfq(List.of(loc));

        PincodeData pinData = new PincodeData();
        pinData.setCity("Bangalore");
        pinData.setState("Karnataka");
        pinData.setPincode("560001");

        when(pincodeDao.findByPincode("560001")).thenReturn(pinData);

        service.resolveDeliveryLocation(rfq, null);
        assertEquals("Bangalore", loc.getCity());
        assertEquals("Karnataka", loc.getState());
    }

    @Test
    void testResolveDeliveryLocation_CityLookup() {
        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("Hyderabad");
        rfq.setClientdeliverylocationrfq(List.of(loc));

        PincodeData pinData = new PincodeData();
        pinData.setCity("Hyderabad");
        pinData.setState("Telangana");
        pinData.setPincode("500001");

        when(pincodeDao.findByCityIgnoreCase("Hyderabad")).thenReturn(pinData);

        service.resolveDeliveryLocation(rfq, null);
        assertEquals("500001", loc.getPincode());
        assertEquals("Telangana", loc.getState());
    }

    @Test
    void testResolveDeliveryLocation_FallbackToOrgAddressWhenNotFound() {
        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("UnknownCity");
        rfq.setClientdeliverylocationrfq(List.of(loc));

        when(pincodeDao.findByCityIgnoreCase("UnknownCity")).thenReturn(null);

        User user = new User();
        Organization org = new Organization();
        org.setCity("FallbackCity");
        org.setState("FallbackState");
        org.setZipCode("999999");
        user.setOrg(org);

        service.resolveDeliveryLocation(rfq, user);
        assertEquals("FallbackCity", loc.getCity());
        assertEquals("FallbackState", loc.getState());
        assertEquals("999999", loc.getPincode());
    }

    @Test
    void testAttachDocuments_Success() {
        Rfq rfq = new Rfq();
        String b64 = Base64.getEncoder().encodeToString("Doc payload".getBytes());
        List<Map<String, String>> docs = List.of(
                Map.of("fileName", "spec.pdf", "file", b64),
                Map.of("fileName", "empty.pdf", "file", ""),
                Collections.singletonMap("fileName", "nullfile.pdf")
        );

        service.attachDocuments(rfq, docs);
        assertNotNull(rfq.getRfqDocument());
        assertEquals(1, rfq.getRfqDocument().size());
        assertEquals("spec.pdf", rfq.getRfqDocument().get(0).getFileName());
    }

    @Test
    void testAttachDocuments_SizeExceeded() {
        Rfq rfq = new Rfq();
        byte[] hugeBytes = new byte[30000000];
        String hugeB64 = Base64.getEncoder().encodeToString(hugeBytes);
        List<Map<String, String>> docs = List.of(
                Map.of("fileName", "huge.pdf", "file", hugeB64)
        );

        assertThrows(RfqDocumentSizeExceededException.class, () -> service.attachDocuments(rfq, docs));
    }

    @Test
    void testAttachDocuments_InvalidBase64() {
        Rfq rfq = new Rfq();
        List<Map<String, String>> docs = List.of(
                Map.of("fileName", "bad.pdf", "file", "Not Valid Base64 %%%")
        );

        assertDoesNotThrow(() -> service.attachDocuments(rfq, docs));
        assertTrue(rfq.getRfqDocument().isEmpty());
    }

    @Test
    void testValidateEmail_UserNotFound() {
        when(userDao.findByLatestUserName("unknown@test.com")).thenReturn(null);

        Map<String, String> response = service.validateEmail("unknown@test.com");
        assertEquals("Failure", response.get("status"));
        assertEquals("No Users Found", response.get("description"));
    }

    @Test
    void testValidateEmail_UserFoundNullOrgId() {
        User user = new User();
        user.setId("U1");
        user.setFullName("Test User");
        Organization org = new Organization();
        user.setOrg(org);

        when(userDao.findByLatestUserName("user@test.com")).thenReturn(user);

        Map<String, String> response = service.validateEmail("user@test.com");
        assertEquals("Success", response.get("status"));
        assertEquals("User Found but Org Id is null", response.get("description"));
    }

    @Test
    void testValidateEmail_UserFoundWithOrgId() {
        User user = new User();
        user.setId("U1");
        user.setFullName("Test User");
        Organization org = new Organization();
        org.setId("ORG1");
        user.setOrg(org);

        when(userDao.findByLatestUserName("user@test.com")).thenReturn(user);

        Map<String, String> response = service.validateEmail("user@test.com");
        assertEquals("Success", response.get("status"));
        assertEquals("ORG1", response.get("orgId"));
    }

    @Test
    void testGenerateRfqId() {
        when(rfqRepository.findByRfqNumber(anyString())).thenReturn(Optional.empty());

        String before = new java.text.SimpleDateFormat("yyddMM").format(new Date());
        String id1 = service.generateRfqId("RFQ");
        String after = new java.text.SimpleDateFormat("yyddMM").format(new Date());
        assertNotNull(id1);
        assertTrue(id1.startsWith("RFQ"));
        assertEquals(15, id1.length());
        assertTrue(id1.matches("^RFQ\\d{12}$"));
        String generatedDate = id1.substring(3, 9);
        assertTrue(generatedDate.equals(before) || generatedDate.equals(after));

        String id2 = service.generateRfqId("AB");
        assertNotNull(id2);
        assertTrue(id2.startsWith("AB"));
        assertEquals(14, id2.length());
        assertTrue(id2.matches("^AB\\d{12}$"));

        String id3 = service.generateRfqId(null);
        assertNotNull(id3);
        assertEquals(12, id3.length());
        assertTrue(id3.matches("^\\d{12}$"));

        String id4 = service.generateRfqId("");
        assertNotNull(id4);
        assertEquals(12, id4.length());
        assertTrue(id4.matches("^\\d{12}$"));
    }

    @Test
    void testRaiseRfq_WithExistingRfqId() {
        Rfq rfq = new Rfq();
        rfq.setRfqId("EXISTING-ID-001");
        List<RfqItem> items = new ArrayList<>();
        RfqItem item = new RfqItem();
        item.setId("ITEM1");
        item.setBrand("Brand");
        item.setDescription("Desc");
        item.setQuantity(10.0);
        item.setRemarks("Remark");
        item.setUnitofMeasures("NOS");
        items.add(item);
        rfq.setRfqItem(items);

        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        boolean result = service.raiseRfq(rfq);
        assertTrue(result);
        assertEquals("EXISTING-ID-001", rfq.getRfqId());
        verify(rfqDao).saveAndFlush(rfq);
    }

    @Test
    void testResolveDeliveryLocation_NullDeliveryInListAndNoCityStatePin() {
        Rfq rfq1 = new Rfq();
        rfq1.setClientdeliverylocationrfq(Collections.singletonList(null));
        assertDoesNotThrow(() -> service.resolveDeliveryLocation(rfq1, null));

        Rfq rfq2 = new Rfq();
        ClientDeliveryLocationRfq blankLoc = new ClientDeliveryLocationRfq();
        blankLoc.setCity("");
        blankLoc.setState("");
        blankLoc.setPincode("");
        rfq2.setClientdeliverylocationrfq(Collections.singletonList(blankLoc));

        User user = new User();
        Organization org = new Organization();
        org.setCity("OrgCity");
        org.setState("OrgState");
        org.setZipCode("111111");
        user.setOrg(org);

        service.resolveDeliveryLocation(rfq2, user);
        assertEquals("OrgCity", blankLoc.getCity());
        assertEquals("OrgState", blankLoc.getState());
        assertEquals("111111", blankLoc.getPincode());
    }

    @Test
    void testAttachDocuments_NullAndEmptyInputs() {
        assertDoesNotThrow(() -> service.attachDocuments(null, List.of()));
        assertDoesNotThrow(() -> service.attachDocuments(new Rfq(), null));
        assertDoesNotThrow(() -> service.attachDocuments(new Rfq(), Collections.emptyList()));

        Rfq rfq = new Rfq();
        List<Map<String, String>> docsWithNullMap = new ArrayList<>();
        docsWithNullMap.add(null);
        assertDoesNotThrow(() -> service.attachDocuments(rfq, docsWithNullMap));
    }

    @Test
    void testRaiseRfq_BranchesAndHelpers() {
        // 1. raiseRfq with null sourceType, null rfqId, qty <= 0, and blank/null uom
        Rfq rfq = new Rfq();
        rfq.setSourceType(null);
        rfq.setRfqId(null);
        RfqItem item1 = new RfqItem();
        item1.setQuantity(0.0);
        item1.setUnitofMeasures(null);
        RfqItem item2 = new RfqItem();
        item2.setQuantity(-5.0);
        item2.setUnitofMeasures("   ");
        rfq.setRfqItem(new ArrayList<>(List.of(item1, item2)));

        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());
        assertTrue(service.raiseRfq(rfq));
        assertEquals("T", rfq.getSourceType());
        assertNotNull(rfq.getRfqId());
        assertEquals(1.0, item1.getQuantity());
        assertEquals("Nos", item1.getUnitofMeasures());
        assertEquals(1.0, item2.getQuantity());
        assertEquals("Nos", item2.getUnitofMeasures());

        // 2. raiseRfq with null rfqItem
        Rfq rfqNullItems = new Rfq();
        rfqNullItems.setSourceType("EMAIL");
        rfqNullItems.setRfqItem(null);
        assertThrows(NullPointerException.class, () -> service.raiseRfq(rfqNullItems));

        // 3. applyOrganizationAddress edge cases
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "applyOrganizationAddress", (ClientDeliveryLocationRfq) null, new User());
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "applyOrganizationAddress", loc, (User) null);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "applyOrganizationAddress", loc, new User());
    }

    @Test
    void testGenerateRfqId_VariationsAndCollisions() {
        // null and short company strings
        String id1 = service.generateRfqId(null);
        assertNotNull(id1);
        String id2 = service.generateRfqId("AB");
        assertNotNull(id2);
        assertTrue(id2.startsWith("AB"));

        // DuplicateKeyException branch in reserveRfqNumber
        when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenReturn(1);
        String id3 = service.generateRfqId("XYZ");
        assertNotNull(id3);
        assertTrue(id3.startsWith("XYZ"));

        // Collision in rfqDao and rfqRepository
        when(rfqDao.findByRfqId(anyString()))
                .thenReturn(new Rfq())
                .thenReturn(null);
        when(rfqRepository.findByRfqNumber(anyString()))
                .thenReturn(Optional.of(new com.portal.procucev.rfq.entity.RFQEntity()))
                .thenReturn(Optional.empty());
        when(jdbcTemplate.update(anyString(), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(0)
                .thenReturn(1);
        String id4 = service.generateRfqId("PRO");
        assertNotNull(id4);
        assertTrue(id4.startsWith("PRO"));
    }

    @Test
    void testResolveDeliveryLocation_NullRfq_And_CityNotFound() {
        assertDoesNotThrow(() -> service.resolveDeliveryLocation(null, new User()));

        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("UnknownCity");
        loc.setState(null);
        loc.setPincode(null);
        rfq.setClientdeliverylocationrfq(new ArrayList<>(List.of(loc)));

        User user = new User();
        Organization org = new Organization();
        org.setCity("OrgCity");
        org.setState("OrgState");
        org.setZipCode("123456");
        user.setOrg(org);

        when(pincodeDao.findByCityIgnoreCase("UnknownCity")).thenReturn(null);
        service.resolveDeliveryLocation(rfq, user);
        assertEquals("OrgCity", loc.getCity());
        assertEquals("OrgState", loc.getState());
        assertEquals("123456", loc.getPincode());
    }

    @Test
    void testAttachDocuments_BlankFileAndInvalidBase64() {
        Rfq rfq = new Rfq();
        Map<String, String> blankFileDoc = new HashMap<>();
        blankFileDoc.put("fileName", "blank.txt");
        blankFileDoc.put("file", "   ");

        Map<String, String> invalidDoc = new HashMap<>();
        invalidDoc.put("fileName", "bad.txt");
        invalidDoc.put("file", "not-valid-base64!!!");

        service.attachDocuments(rfq, List.of(blankFileDoc, invalidDoc));
        assertTrue(rfq.getRfqDocument().isEmpty());
    }

    @Test
    void testValidateEmail_OrgIdNull() {
        User userWithNullOrgId = new User();
        userWithNullOrgId.setFullName("John Doe");
        userWithNullOrgId.setId("U123");
        Organization org = new Organization();
        org.setId(null);
        userWithNullOrgId.setOrg(org);

        when(userDao.findByLatestUserName("test@nullorg.com")).thenReturn(userWithNullOrgId);
        Map<String, String> resp = service.validateEmail("test@nullorg.com");
        assertEquals("Success", resp.get("status"));
        assertEquals("00", resp.get("code"));
        assertNull(resp.get("orgId"));
        assertEquals("User Found but Org Id is null", resp.get("description"));
    }
}
