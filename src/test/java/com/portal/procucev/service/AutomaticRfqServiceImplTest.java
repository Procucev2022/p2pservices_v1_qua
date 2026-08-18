package com.portal.procucev.service;

import com.portal.procucev.dao.GmtItemsDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.RfqDao;
import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AutomaticRfqServiceImpl service;

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

        // The RFQ must be written and flushed BEFORE the GMT items are built, because building them
        // reads each RfqItem's UUID and that is only assigned once the cascaded insert has run.
        // Building them first wrote every gmt_items row with rfq_item_id = NULL.
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
    void testRaiseRfq_Exception() {
        Rfq rfq = new Rfq();
        rfq.setRfqItem(new ArrayList<>());
        when(masterStatusDao.findByStatus(anyString())).thenThrow(new DataIntegrityViolationException("Error"));

        boolean result = service.raiseRfq(rfq);
        assertFalse(result);
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
        String id1 = service.generateRfqId("RFQ");
        assertNotNull(id1);
        assertTrue(id1.startsWith("RFQ"));

        String id2 = service.generateRfqId("AB");
        assertNotNull(id2);
        assertTrue(id2.startsWith("AB"));

        String id3 = service.generateRfqId(null);
        assertNotNull(id3);

        String id4 = service.generateRfqId("");
        assertNotNull(id4);
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
    void testRaiseRfq_WithBlankRfqId_GeneratesNew() {
        Rfq rfq = new Rfq();
        rfq.setRfqId("");
        List<RfqItem> items = new ArrayList<>();
        RfqItem item = new RfqItem();
        item.setId("ITEM2");
        item.setBrand("B");
        item.setDescription("D");
        item.setQuantity(1.0);
        item.setRemarks("R");
        item.setUnitofMeasures("U");
        items.add(item);
        rfq.setRfqItem(items);

        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        boolean result = service.raiseRfq(rfq);
        assertTrue(result);
        assertNotNull(rfq.getRfqId());
        assertFalse(rfq.getRfqId().isBlank());
    }

    @Test
    void testRaiseRfq_WithNullRfqId_GeneratesNew() {
        Rfq rfq = new Rfq();
        rfq.setRfqId(null);
        List<RfqItem> items = new ArrayList<>();
        RfqItem item = new RfqItem();
        item.setId("ITEM3");
        item.setBrand("B");
        item.setDescription("D");
        item.setQuantity(1.0);
        item.setRemarks("R");
        item.setUnitofMeasures("U");
        items.add(item);
        rfq.setRfqItem(items);

        when(masterStatusDao.findByStatus(anyString())).thenReturn(new MasterStatus());

        boolean result = service.raiseRfq(rfq);
        assertTrue(result);
        assertNotNull(rfq.getRfqId());
        assertFalse(rfq.getRfqId().isBlank());
    }

    @Test
    void testRaiseRfq_SetsCorrectFlags() {
        Rfq rfq = new Rfq();
        rfq.setRfqId("RFQ-FLAG-TEST");
        List<RfqItem> items = new ArrayList<>();
        RfqItem item = new RfqItem();
        item.setId("ITEM4");
        item.setBrand("B");
        item.setDescription("D");
        item.setQuantity(5.0);
        item.setRemarks("R");
        item.setUnitofMeasures("U");
        items.add(item);
        rfq.setRfqItem(items);

        MasterStatus ms = new MasterStatus();
        when(masterStatusDao.findByStatus(anyString())).thenReturn(ms);

        boolean result = service.raiseRfq(rfq);
        assertTrue(result);
        assertTrue(rfq.isByClient());
        assertEquals(ms, rfq.getStatus());
        assertEquals(ms, rfq.getClientStatus());
    }

    @Test
    void testValidateEmail_Exception() {
        when(userDao.findByLatestUserName("error@test.com")).thenThrow(new RuntimeException("DB Error"));
        assertThrows(RuntimeException.class, () -> service.validateEmail("error@test.com"));
    }
}

