package com.portal.procucev.service;

import com.portal.procucev.customexception.RfqDocumentSizeExceededException;
import com.portal.procucev.dao.GmtItemsDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.PincodeDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.PincodeData;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.User;
import com.portal.procucev.rfq.repository.RFQRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Covers the RFQ creation behaviour that the web upload and the email-to-RFQ pipeline now share:
 * document mapping, the per-document size limit, delivery-location resolution and quantity
 * defaulting.
 */
@ExtendWith(MockitoExtension.class)
class SharedRfqCreationPipelineTest {

    private static final long MAX_BYTES = 26214400L;

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

    private Map<String, String> document(String fileName, byte[] payload) {
        Map<String, String> doc = new HashMap<>();
        doc.put("fileName", fileName);
        doc.put("file", Base64.getEncoder().encodeToString(payload));
        return doc;
    }

    /**
     * The defect this guards: writing the payload into file_details as well as file made MySQL
     * reject the insert with "Data too long for column 'file_details'" for anything over 64KB,
     * because that column is a BLOB. A 1.45MB drawing sent by email rolled the whole RFQ back while
     * the same file uploaded through the web succeeded.
     */
    @Test
    @DisplayName("attachDocuments stores the payload in file only and leaves the 64KB file_details column unset")
    void attachDocumentsLeavesFileDetailsUnset() {
        Rfq rfq = new Rfq();
        byte[] payload = new byte[200_000];
        Arrays.fill(payload, (byte) 7);

        service.attachDocuments(rfq, List.of(document("drawing.jpg", payload)));

        assertEquals(1, rfq.getRfqDocument().size());
        RFQDocument stored = rfq.getRfqDocument().get(0);
        assertEquals("drawing.jpg", stored.getFileName());
        assertNotNull(stored.getFile());
        assertEquals(payload.length, stored.getFile().length);
        assertNull(stored.getFileDetails(),
                "file_details is a 64KB BLOB and is read by nothing; populating it is what broke the insert");
        assertEquals(Integer.valueOf(1), stored.getVersion());
        assertEquals(rfq, stored.getRfq());
    }

    @Test
    @DisplayName("attachDocuments accepts a document at exactly the configured limit, matching the web upload")
    void attachDocumentsAcceptsDocumentAtLimit() {
        ReflectionTestUtils.setField(service, "maxDocumentBytes", 1024L);
        Rfq rfq = new Rfq();

        service.attachDocuments(rfq, List.of(document("at-limit.pdf", new byte[1024])));

        assertEquals(1, rfq.getRfqDocument().size());
        assertEquals(1024, rfq.getRfqDocument().get(0).getFile().length);
    }

    @Test
    @DisplayName("attachDocuments rejects a document over the configured limit")
    void attachDocumentsRejectsOversizedDocument() {
        ReflectionTestUtils.setField(service, "maxDocumentBytes", 1024L);
        Rfq rfq = new Rfq();

        RfqDocumentSizeExceededException thrown = assertThrows(RfqDocumentSizeExceededException.class,
                () -> service.attachDocuments(rfq, List.of(document("huge.pdf", new byte[2048]))));

        assertEquals("huge.pdf", thrown.getFileName());
        assertEquals(2048L, thrown.getFileSizeBytes());
        assertEquals(1024L, thrown.getMaxAllowedBytes());
    }

    @Test
    @DisplayName("attachDocuments defaults to the 25MB limit shared with spring.servlet.multipart.max-file-size")
    void attachDocumentsDefaultsToWebLimit() {
        assertEquals(MAX_BYTES, service.getMaxDocumentBytes());
    }

    @Test
    @DisplayName("attachDocuments skips entries with no payload and entries that are not valid Base64")
    void attachDocumentsSkipsUnusableEntries() {
        Rfq rfq = new Rfq();
        Map<String, String> blank = new HashMap<>();
        blank.put("fileName", "blank.txt");
        blank.put("file", "");
        Map<String, String> notBase64 = new HashMap<>();
        notBase64.put("fileName", "broken.txt");
        notBase64.put("file", "!!!not-base64!!!");

        service.attachDocuments(rfq, Arrays.asList(blank, notBase64, document("good.txt", "ok".getBytes())));

        assertEquals(1, rfq.getRfqDocument().size());
        assertEquals("good.txt", rfq.getRfqDocument().get(0).getFileName());
    }

    @Test
    @DisplayName("attachDocuments tolerates a null or empty document list")
    void attachDocumentsTolerartesNoDocuments() {
        Rfq rfq = new Rfq();

        service.attachDocuments(rfq, null);
        service.attachDocuments(rfq, Collections.emptyList());

        assertTrue(rfq.getRfqDocument().isEmpty());
    }

    @Test
    @DisplayName("raiseRfq defaults a missing quantity to 1 and a missing unit of measure to Nos")
    void raiseRfqDefaultsMissingQuantityToOne() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        Rfq rfq = new Rfq();
        RfqItem noQuantity = new RfqItem();
        noQuantity.setDescription("Aluminum Sealing Ring Washer");
        // Left at the primitive default, which is what an absent quantity deserialises to and what
        // the email pipeline passes through when extraction returned none.
        RfqItem negativeQuantity = new RfqItem();
        negativeQuantity.setDescription("Plain Washer");
        negativeQuantity.setQuantity(-5.0);
        rfq.setRfqItem(new ArrayList<>(List.of(noQuantity, negativeQuantity)));

        assertTrue(service.raiseRfq(rfq));

        assertEquals(1.0, rfq.getRfqItem().get(0).getQuantity());
        assertEquals("Nos", rfq.getRfqItem().get(0).getUnitofMeasures());
        assertEquals(1.0, rfq.getRfqItem().get(1).getQuantity());
    }

    @Test
    @DisplayName("raiseRfq preserves a stated quantity")
    void raiseRfqPreservesStatedQuantity() {
        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        Rfq rfq = new Rfq();
        RfqItem item = new RfqItem();
        item.setDescription("Aluminum Sealing Ring Washer");
        item.setQuantity(40000.0);
        item.setUnitofMeasures("nos");
        rfq.setRfqItem(new ArrayList<>(List.of(item)));

        assertTrue(service.raiseRfq(rfq));

        assertEquals(40000.0, rfq.getRfqItem().get(0).getQuantity());
        assertEquals("nos", rfq.getRfqItem().get(0).getUnitofMeasures());
    }

    @Test
    @DisplayName("resolveDeliveryLocation completes city and state from the pincode master")
    void resolveDeliveryLocationCompletesFromPincode() {
        when(pincodeDao.findByPincode("590008")).thenReturn(new PincodeData("590008", "Belagavi", "Karnataka"));

        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq location = new ClientDeliveryLocationRfq();
        location.setPincode("590008");
        rfq.setClientdeliverylocationrfq(new ArrayList<>(List.of(location)));

        service.resolveDeliveryLocation(rfq, userWithOrg("Bengaluru", "Karnataka", "560001"));

        assertEquals("Belagavi", location.getCity());
        assertEquals("Karnataka", location.getState());
        assertEquals("590008", location.getPincode());
    }

    @Test
    @DisplayName("resolveDeliveryLocation falls back to the organisation address when nothing is supplied")
    void resolveDeliveryLocationFallsBackToOrganisation() {
        Rfq rfq = new Rfq();
        rfq.setClientdeliverylocationrfq(null);

        service.resolveDeliveryLocation(rfq, userWithOrg("Bengaluru", "Karnataka", "560001"));

        assertEquals(1, rfq.getClientdeliverylocationrfq().size());
        ClientDeliveryLocationRfq resolved = rfq.getClientdeliverylocationrfq().get(0);
        assertEquals("Bengaluru", resolved.getCity());
        assertEquals("Karnataka", resolved.getState());
        assertEquals("560001", resolved.getPincode());
    }

    @Test
    @DisplayName("resolveDeliveryLocation leaves a fully supplied address untouched")
    void resolveDeliveryLocationLeavesCompleteAddressAlone() {
        Rfq rfq = new Rfq();
        ClientDeliveryLocationRfq location = new ClientDeliveryLocationRfq();
        location.setCity("Belagavi");
        location.setState("Karnataka");
        location.setPincode("590008");
        rfq.setClientdeliverylocationrfq(new ArrayList<>(List.of(location)));

        service.resolveDeliveryLocation(rfq, userWithOrg("Bengaluru", "Karnataka", "560001"));

        assertEquals("Belagavi", location.getCity());
        assertEquals("Karnataka", location.getState());
        assertEquals("590008", location.getPincode());
    }

    private User userWithOrg(String city, String state, String zip) {
        Organization org = new Organization();
        org.setCity(city);
        org.setState(state);
        org.setZipCode(zip);
        User user = new User();
        user.setOrg(org);
        return user;
    }
}
