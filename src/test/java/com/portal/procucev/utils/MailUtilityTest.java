package com.portal.procucev.utils;

import com.portal.procucev.customexception.AppException;
import com.portal.procucev.model.BFSUsers;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.model.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailUtilityTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    private InternetAddress internetAddress;
    private User user;
    private Organization org;

    @BeforeEach
    void setUp() throws Exception {
        internetAddress = new InternetAddress("test@procucev.com");
        user = new User();
        user.setUsername("testuser");
        user.setPassword("pass123");
        user.setPhone("9876543210");
        user.setCompanyName("Company A");
        org = new Organization();
        org.setId("ORG123");
        org.setCompanyName("Test Company");
        org.setEmail("org@test.com");
        org.setAddress1("Address 1");
        org.setOrganizationPhonenumber("9876543210");
        user.setOrg(org);

        lenient().when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testInstantiation() {
        MailUtility mailUtility = new MailUtility();
        assertNotNull(mailUtility);
    }

    @Test
    void testEmailVendorApprovedStatus() {
        MailUtility.emailVendorApprovedStatus("TYPE", "to@test.com", javaMailSender, internetAddress, "http://host", user);
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendEmailMessage_Failure() {
        doThrow(new RuntimeException("mail err")).when(javaMailSender).send(any(MimeMessage.class));
        boolean res = MailUtility.sendEmailMessage("to@test.com", internetAddress, javaMailSender, "msg", "type");
        assertFalse(res);
    }

    @Test
    void testSendEmailMessageforReject() {
        boolean res = MailUtility.sendEmailMessageforReject("to@test.com", "from@test.com", javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testEmailVendorRejectedStatus() {
        MailUtility.emailVendorRejectedStatus("type", "user@test.com", javaMailSender, "from@test.com", "http://host", user);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailMessageforNewPR() {
        boolean res = MailUtility.sendEmailMessageforNewPR("to@test.com", internetAddress, javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testSendemailForNewRfq() {
        boolean res = MailUtility.sendemailForNewRfq("to@test.com", internetAddress, javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testRfqAccept() {
        boolean res = MailUtility.rfqAccept("to@test.com", internetAddress, javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testEmailrfqAccept() {
        MailUtility.emailrfqAccept("type", "user@test.com", javaMailSender, internetAddress, "RFQ1", "http://host", user);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEamilfornewVendor() {
        boolean res = MailUtility.eamilfornewVendor("to@test.com", internetAddress, javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testEmailnewVendor() {
        MailUtility.emailnewVendor("type", "user@test.com", javaMailSender, internetAddress, "V1", "http://host", user);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmailforforgotpassword() {
        boolean res = MailUtility.emailforforgotpassword("to@test.com", internetAddress, javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testEmailforgotpassword() {
        MailUtility.emailforgotpassword("type", "user@test.com", javaMailSender, internetAddress, "pass", "http://host", user);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testCancelAuctionInvitation() {
        MailUtility.cancelAuctionInvitation("type", "user@test.com", javaMailSender, "from@test.com", "AUC1", "http://host", user);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendemail() {
        boolean res = MailUtility.sendemail("to@test.com", internetAddress, javaMailSender, "msg", "type", "subj");
        assertTrue(res);
    }

    @Test
    void testEmailNotifierGenericBySenderList() {
        boolean res = MailUtility.emailNotifierGenericBySenderList("subj", Collections.singletonList("to@test.com"), internetAddress, javaMailSender, "msg", "type");
        assertTrue(res);
    }

    @Test
    void testEmailVendorApprovedAndRejectedStatus() {
        MailUtility.emailVendorApprovedStatus("type", "to@test.com", javaMailSender, internetAddress, "host", user);
        MailUtility.emailVendorRejectedStatus("type", "to@test.com", javaMailSender, "from@test.com", "host", user);

        JavaMailSender failingSender = mock(JavaMailSender.class);
        when(failingSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("mail err")).when(failingSender).send(any(MimeMessage.class));

        assertFalse(MailUtility.sendEmailMessage("to@test.com", internetAddress, failingSender, "msg", "type"));
        assertFalse(MailUtility.sendEmailMessageforReject("to@test.com", "from@test.com", failingSender, "msg", "type"));
        assertFalse(MailUtility.sendEmailMessageforNewPR("to@test.com", internetAddress, failingSender, "msg", "type"));
        assertFalse(MailUtility.sendemailForNewRfq("to@test.com", internetAddress, failingSender, "msg", "type"));
        assertFalse(MailUtility.eamilfornewVendor("to@test.com", internetAddress, failingSender, "msg", "type"));
    }

    @Test
    void testNotifyClientForNewComment() {
        MailUtility.notifyClientForNewComment("type", Collections.singletonList("user1"), javaMailSender, internetAddress, "s2", "http://host", "DEL1");
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testNotifyVendorForNewComment() {
        MailUtility.notifyVendorForNewComment("type", Collections.singletonList("user1"), javaMailSender, internetAddress, "id", "http://host", "DEL1");
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmailVendorDBDownloadByVendorExc() {
        MailUtility.emailVendorDBDownloadByVendorExc("type", "user1@test.com", javaMailSender, internetAddress, "http://host");
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testReminderMailWithUserCred() {
        MailUtility.reminderMailWithUserCred("type", "user1@test.com", javaMailSender, internetAddress, "http://host", user);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmailSelfRegisterVendor() {
        MailUtility.emailSelfRegisterVendor("type", Collections.singletonList("id1"), javaMailSender, internetAddress, "Comp", "http://host", null);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmailSelfRegisterVendorAcceptance() {
        MailUtility.emailSelfRegisterVendorAcceptance("type", Collections.singletonList("id1"), javaMailSender, internetAddress, Collections.singletonList("Comp1"), "http://host", null);
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmaildailyRFQStatus() {
        Rfq rfq = new Rfq();
        rfq.setRfqId("RFQ001");
        RfqItem item = new RfqItem();
        item.setDescription("Item 1");
        rfq.setRfqItem(Collections.singletonList(item));

        MailUtility.emaildailyRFQStatus("type", Collections.singletonList("id1"), javaMailSender, internetAddress, Collections.singletonList(rfq), "http://host");
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testEmailNewRfq() {
        Rfq rfq = new Rfq();
        rfq.setRfqId("RFQ001");
        RfqItem item = new RfqItem();
        item.setSerialNo(1);
        item.setDescription("Item 1");
        item.setBrand("Brand A");
        item.setQuantity(10.0);
        item.setUnitofMeasures("PCS");
        rfq.setRfqItem(Collections.singletonList(item));

        MailUtility.emailNewRfq("type", javaMailSender, internetAddress, "RFQ001", "http://host", user, rfq, "cc@test.com", "99999", "cc@test.com");
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void testMailingGMTClientRFQMailToinfoTeam() {
        assertDoesNotThrow(() -> MailUtility.mailingGMTClientRFQMailToinfoTeam("type", "to@test.com", javaMailSender, internetAddress, "http://host", user, "u@test.com", "9876543210", "Full Name", "Org"));
    }

    @Test
    void testMailingVerificationLinkWithUserLogin() {
        assertDoesNotThrow(() -> MailUtility.mailingVerificationLinkWithUserLogin(javaMailSender, "from@test.com", internetAddress, "pass", "http://host", user));
    }

    @Test
    void testMailingVerificationLinkWithSelfUserLogin() {
        assertDoesNotThrow(() -> MailUtility.mailingVerificationLinkWithSelfUserLogin(javaMailSender, "from@test.com", internetAddress, "pass", "http://host", user));
    }

    @Test
    void testSendClientEmailForCM2() {
        assertDoesNotThrow(() -> MailUtility.sendClientEmailForCM2("type", "to@test.com", org, javaMailSender, internetAddress, "http://host"));
    }

    @Test
    void testNewBidRequestMethods() {
        BFSUsers bfsUser = new BFSUsers();
        com.portal.procucev.model.BFSItems itemCat = new com.portal.procucev.model.BFSItems();
        itemCat.setDescription("Desc 1");
        bfsUser.setItems(itemCat);
        bfsUser.setAskPrice(100.0);

        MailUtility.emailForBidRequest("type", "to@test.com", javaMailSender, internetAddress, "http://host", bfsUser, user, "Desc 1");
        MailUtility.sendVendorEmailForCM2("type", "to@test.com", org, javaMailSender, internetAddress, "http://host");
        MailUtility.emailrfqReject("type", "user@test.com", javaMailSender, internetAddress, "RFQ1", "http://host", user);
        MailUtility.mailingVerificationLinkWithUser(javaMailSender, internetAddress, "http://host", user);
        MailUtility.emailForBuyerBidRequest("type", "buyer@test.com", javaMailSender, internetAddress, "http://host", bfsUser, user, "Desc 1");
        MailUtility.emailForsellerBidRequest("type", "seller@test.com", javaMailSender, internetAddress, "http://host", bfsUser, user, "Desc 1");
    }

    @Test
    void testNoPrEmailMethods() throws Exception {
        Rfq rfq = new Rfq();
        rfq.setRfqId("RFQ123");
        rfq.setProjectDesc("Project Desc");

        ClientDeliveryLocationRfq loc = new ClientDeliveryLocationRfq();
        loc.setCity("Bengaluru");
        loc.setPincode("560001");
        rfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        RfqItem item = new RfqItem();
        item.setDescription("Item 1");
        item.setBrand("Brand A");
        item.setQuantity(10.0);
        item.setUnitofMeasures("PCS");
        item.setRemarks("Remarks 1");
        rfq.setRfqItem(Collections.singletonList(item));

        RFQDocument doc = new RFQDocument();
        doc.setFileName("doc.pdf");
        doc.setFile(new byte[]{1, 2, 3});
        rfq.setRfqDocument(Collections.singletonList(doc));

        assertDoesNotThrow(() -> MailUtility.emailNewRfqForNoPR(
                "Subject", "type", javaMailSender, rfq, "http://host",
                "to@test.com", "from@test.com", "cc@test.com", "9876543210",
                "2026-12-31", "Full Name", "mail2@test.com", "pass2", "V100", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailNewRfqForNoPRForExistingUsers(
                "Subject", "type", javaMailSender, rfq, "http://host",
                "to@test.com", "from@test.com", "cc@test.com", "9876543210",
                "2026-12-31", "Full Name", "mail2@test.com", "pass2", "V100", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailInviteRfq(
                javaMailSender, rfq, "http://host", "to@test.com",
                "from@test.com", "cc@test.com", "9876543210", "Full Name",
                "mail2@test.com", "pass2", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailInviteRfqForExistingUsers(
                javaMailSender, rfq, "http://host", "to@test.com",
                "from@test.com", "cc@test.com", "9876543210", "Full Name",
                "mail2@test.com", "pass2", "9876543210"
        ));

        Rfq emptyRfq = new Rfq();
        emptyRfq.setRfqId("RFQ124");
        emptyRfq.setClientdeliverylocationrfq(null);
        emptyRfq.setRfqDocument(null);
        emptyRfq.setRfqItem(Collections.emptyList());

        assertDoesNotThrow(() -> MailUtility.emailNewRfqForNoPR(
                "Subject", "type", javaMailSender, emptyRfq, "http://host",
                "to@test.com", "from@test.com", null, "9876543210",
                "2026-12-31", "Full Name", "mail2@test.com", "pass2", "V100", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailNewRfqForNoPRForExistingUsers(
                "Subject", "type", javaMailSender, emptyRfq, "http://host",
                "to@test.com", "from@test.com", null, "9876543210",
                "2026-12-31", "Full Name", "mail2@test.com", "pass2", "V100", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailInviteRfq(
                javaMailSender, emptyRfq, "http://host", "to@test.com",
                "from@test.com", null, "9876543210", "Full Name",
                "mail2@test.com", "pass2", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailInviteRfqForExistingUsers(
                javaMailSender, emptyRfq, "http://host", "to@test.com",
                "from@test.com", null, "9876543210", "Full Name",
                "mail2@test.com", "pass2", "9876543210"
        ));

        assertDoesNotThrow(() -> MailUtility.emailNewGMTRfqForNoPR(
                "type", "Subject", javaMailSender, rfq, "http://host",
                "vendor@test.com", "other@test.com", "from@test.com", "pass",
                "2026-12-31", "V100"
        ));

        assertDoesNotThrow(() -> MailUtility.emailNewGMTRfqForNoPR(
                "type", "Subject", javaMailSender, emptyRfq, "http://host",
                "vendor@test.com", null, "from@test.com", "pass",
                "2026-12-31", "V100"
        ));

        MailUtility.sendOtpForEmail("type", "user@test.com", javaMailSender, internetAddress, "http://host", "123456");
        MailUtility.sendEmailForClient("type", "user@test.com", javaMailSender, internetAddress, "http://host");
        MailUtility.emailForVendor("type", "user@test.com", javaMailSender, internetAddress, "http://host");
        MailUtility.emailPPOForApproval("type", "user@test.com", javaMailSender, internetAddress, "PPO1", "http://host", "type");

        BFSUsers bfsUser = new BFSUsers();
        bfsUser.setUniqueId("UNIQ1");
        bfsUser.setAskPrice(100.0);
        com.portal.procucev.model.BFSItems bfsItem = new com.portal.procucev.model.BFSItems();
        bfsItem.setItemNumber("ITM1");
        bfsItem.setDescription("Item 1");
        bfsUser.setItems(bfsItem);
        bfsUser.setQuantity(10);

        MailUtility.buyerEmailBFSAccepted("type", "user@test.com", javaMailSender, internetAddress, bfsUser, "http://host", "UNIQ1");
        MailUtility.emailBFSAccepted("type", "user@test.com", javaMailSender, internetAddress, bfsUser, "http://host", "UNIQ1");
        MailUtility.sellerEmailBFSAccepted("type", "user@test.com", javaMailSender, internetAddress, bfsUser, "http://host", "UNIQ1");

        MailUtility.sendClientEmailForCM2("type", "user@test.com", org, javaMailSender, internetAddress, "http://host");
        MailUtility.emailrfqReject("type", "user@test.com", javaMailSender, internetAddress, "RFQ1", "http://host", user);

        // Branch tests for null/empty lists & Multipart content
        MailUtility.emaildailyRFQStatus("type", Collections.singletonList("id1"), javaMailSender, internetAddress, Collections.singletonList(rfq), "http://host");
        MailUtility.emailSelfRegisterVendorAcceptance("type", Collections.singletonList("id1"), javaMailSender, internetAddress, Collections.singletonList("Company ABC"), "http://host", null);

        Rfq emptyItemsRfq = new Rfq();
        emptyItemsRfq.setRfqId("RFQ999");
        emptyItemsRfq.setRfqItem(Collections.emptyList());
        MailUtility.emaildailyRFQStatus("type", Collections.singletonList("id1"), javaMailSender, internetAddress, Collections.singletonList(emptyItemsRfq), "http://host");
        MailUtility.emailNewRfq("type", javaMailSender, internetAddress, "RFQ999", "http://host", user, emptyItemsRfq, "user@test.com", "9999999999", "cc@test.com");
        MailUtility.emailNewRfq("type", javaMailSender, internetAddress, "RFQ999", "http://host", user, emptyItemsRfq, "cc@test.com", "99999", "");
    }

    @Test
    void testBfsAcceptedMails_WithItemNumber() throws Exception {
        com.portal.procucev.model.BFSItems items = new com.portal.procucev.model.BFSItems();
        items.setItemNumber("10");
        items.setDescription("Desc");
        BFSUsers bfsUser = new BFSUsers();
        bfsUser.setItems(items);
        bfsUser.setAskPrice(100.0);

        assertDoesNotThrow(() -> MailUtility.buyerEmailBFSAccepted("type", "to@test.com", javaMailSender, internetAddress, bfsUser, "host", "UNIQ1"));
        assertDoesNotThrow(() -> MailUtility.emailBFSAccepted("type", "to@test.com", javaMailSender, internetAddress, bfsUser, "host", "UNIQ1"));
        assertDoesNotThrow(() -> MailUtility.sellerEmailBFSAccepted("type", "to@test.com", javaMailSender, internetAddress, bfsUser, "host", "UNIQ1"));

        assertDoesNotThrow(() -> MailUtility.emailForVendor("type", "to@test.com", javaMailSender, internetAddress, "host"));

        com.portal.procucev.model.RfqItem rfqItem = new com.portal.procucev.model.RfqItem();
        rfqItem.setBrand("Brand");
        rfqItem.setUnitofMeasures("PCS");
        rfqItem.setQuantity(10.0);
        rfqItem.setRemarks("Rem");
        Rfq itemRfq = new Rfq();
        itemRfq.setRfqId("RFQ10");
        itemRfq.setRfqItem(Collections.singletonList(rfqItem));
        assertDoesNotThrow(() -> MailUtility.mailingVerificationLinkWithUserLogin(javaMailSender, "from@test.com", internetAddress, "pass", "host", user));
        assertDoesNotThrow(() -> MailUtility.cancelAuctionInvitation("type", "to@test.com", javaMailSender, "from@test.com", "AUC1", "host", user));
        assertDoesNotThrow(() -> MailUtility.sendemail("to@test.com", internetAddress, javaMailSender, "msg", "type", "subj"));
        assertDoesNotThrow(() -> MailUtility.notifyClientForNewComment("type", Collections.singletonList("to@test.com"), javaMailSender, internetAddress, "s2", "host", "DEL1"));
        assertDoesNotThrow(() -> MailUtility.notifyVendorForNewComment("type", Collections.singletonList("to@test.com"), javaMailSender, internetAddress, "id", "host", "DEL1"));
        assertDoesNotThrow(() -> MailUtility.emailVendorDBDownloadByVendorExc("type", "to@test.com", javaMailSender, internetAddress, "host"));
        assertDoesNotThrow(() -> MailUtility.reminderMailWithUserCred("type", "to@test.com", javaMailSender, internetAddress, "host", user));
        assertDoesNotThrow(() -> MailUtility.emailSelfRegisterVendor("type", Collections.singletonList("to@test.com"), javaMailSender, internetAddress, "Comp", "host", null));
        assertDoesNotThrow(() -> MailUtility.emailNewRfq("type", javaMailSender, internetAddress, "RFQ10", "host", user, itemRfq, "user@test.com", "99999", "cc@test.com"));
        assertDoesNotThrow(() -> MailUtility.emailNewRfq("type", javaMailSender, internetAddress, "RFQ10", "host", user, itemRfq, "user@test.com", "99999", null));
        assertDoesNotThrow(() -> MailUtility.emailNewRfq("type", javaMailSender, internetAddress, "RFQ10", "host", user, itemRfq, "user@test.com", "99999", ""));

        com.portal.procucev.model.ClientDeliveryLocationRfq loc = new com.portal.procucev.model.ClientDeliveryLocationRfq();
        loc.setCity("CityA");
        loc.setPincode("560001");
        itemRfq.setClientdeliverylocationrfq(Collections.singletonList(loc));

        RFQDocument doc = new RFQDocument();
        doc.setFileName("file.pdf");
        doc.setFile("content".getBytes());
        itemRfq.setRfqDocument(Collections.singletonList(doc));

        assertDoesNotThrow(() -> MailUtility.emailNewRfqForNoPR("Prefix", "s", javaMailSender, itemRfq, "host", "to@test.com", "from@test.com", "cc@test.com", "99999", "2026-12-31", "Full Name", "from@test.com", "pass", "V100", "9876543210"));
        assertDoesNotThrow(() -> MailUtility.emailNewRfqForNoPRForExistingUsers("Prefix", "s", javaMailSender, itemRfq, "host", "to@test.com", "from@test.com", "cc@test.com", "99999", "2026-12-31", "Full Name", "from@test.com", "pass", "V100", "9876543210"));
        assertDoesNotThrow(() -> MailUtility.emailInviteRfq(javaMailSender, itemRfq, "host", "to@test.com", "from@test.com", "cc@test.com", "99999", "Full Name", "from@test.com", "pass", "9876543210"));
        assertDoesNotThrow(() -> MailUtility.emailInviteRfqForExistingUsers(javaMailSender, itemRfq, "host", "to@test.com", "from@test.com", "cc@test.com", "99999", "Full Name", "from@test.com", "pass", "9876543210"));
        assertDoesNotThrow(() -> MailUtility.emailNewGMTRfqForNoPR("str", "Prefix", javaMailSender, itemRfq, "host", "vendor@test.com", "other@test.com", "from@test.com", "pass", "2026-12-31", "V100"));
        assertDoesNotThrow(() -> MailUtility.sendOtpForEmail("type", "to@test.com", javaMailSender, internetAddress, "host", "123456"));
        assertDoesNotThrow(() -> MailUtility.sendEmailForClient("type", "to@test.com", javaMailSender, internetAddress, "host"));
        assertDoesNotThrow(() -> MailUtility.emailForVendor("type", "to@test.com", javaMailSender, internetAddress, "host"));
        assertDoesNotThrow(() -> MailUtility.mailingGMTClientRFQMailToinfoTeam("type", "to@test.com", javaMailSender, internetAddress, "host", user, "user@test.com", "99999", "Name", "Org"));
        assertDoesNotThrow(() -> MailUtility.emailPPOForApproval("type", "to@test.com", javaMailSender, internetAddress, "PPO10", "host", "type"));

        com.portal.procucev.model.BFSUsers bfsUser2 = new com.portal.procucev.model.BFSUsers();
        bfsUser2.setAskPrice(100.0);
        com.portal.procucev.model.BFSItems bfsItem = new com.portal.procucev.model.BFSItems();
        bfsItem.setItemNumber("ITEM100");
        bfsItem.setBfsGroup("GRP1");
        bfsItem.setDescription("Item Desc");
        bfsItem.setTotalQuantity(10.0);
        bfsUser2.setItems(bfsItem);
        assertDoesNotThrow(() -> MailUtility.buyerEmailBFSAccepted("type", "to@test.com", javaMailSender, internetAddress, bfsUser2, "host", "U100"));
        assertDoesNotThrow(() -> MailUtility.emailBFSAccepted("type", "to@test.com", javaMailSender, internetAddress, bfsUser2, "host", "U100"));
        assertDoesNotThrow(() -> MailUtility.sellerEmailBFSAccepted("type", "to@test.com", javaMailSender, internetAddress, bfsUser2, "host", "U100"));

        com.portal.procucev.model.Organization testOrg = new com.portal.procucev.model.Organization();
        testOrg.setCompanyName("Test Org");
        testOrg.setEmail("org@test.com");
        testOrg.setOrganizationPhonenumber("9999999999");
        assertDoesNotThrow(() -> MailUtility.sendClientEmailForCM2("type", "to@test.com", testOrg, javaMailSender, internetAddress, "host"));

        // mailingVerificationLinkWithSelfUserLogin
        assertDoesNotThrow(() -> MailUtility.mailingVerificationLinkWithSelfUserLogin(javaMailSender, "from@test.com", internetAddress, "pass", "host", user));

        // emailForBidRequest
        com.portal.procucev.model.BFSUsers bidBfsUser = new com.portal.procucev.model.BFSUsers();
        bidBfsUser.setAskPrice(200.0);
        com.portal.procucev.model.BFSItems bidBfsItem = new com.portal.procucev.model.BFSItems();
        bidBfsItem.setDescription("Bid Item");
        bidBfsUser.setItems(bidBfsItem);
        user.setCompanyName("Company XYZ");
        assertDoesNotThrow(() -> MailUtility.emailForBidRequest("type", "to@test.com", javaMailSender, internetAddress, "host", bidBfsUser, user, "desc"));

        // sendVendorEmailForCM2
        assertDoesNotThrow(() -> MailUtility.sendVendorEmailForCM2("type", "to@test.com", testOrg, javaMailSender, internetAddress, "host"));

        // mailingVerificationLinkWithUser
        assertDoesNotThrow(() -> MailUtility.mailingVerificationLinkWithUser(javaMailSender, internetAddress, "host", user));
        JavaMailSender badSender = mock(JavaMailSender.class);
        when(badSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("err")).when(badSender).send(any(MimeMessage.class));
        assertThrows(AppException.class, () -> MailUtility.mailingVerificationLinkWithUser(badSender, internetAddress, "host", user));

        // emailForBuyerBidRequest & emailForsellerBidRequest
        assertDoesNotThrow(() -> MailUtility.emailForBuyerBidRequest("type", "to@test.com", javaMailSender, internetAddress, "host", bidBfsUser, user, "desc"));
        assertDoesNotThrow(() -> MailUtility.emailForsellerBidRequest("type", "to@test.com", javaMailSender, internetAddress, "host", bidBfsUser, user, "desc"));

        // Direct helper tests
        assertTrue(MailUtility.sendEmailMessageforNewPR("to@test.com", internetAddress, javaMailSender, "msg", "type"));
        assertTrue(MailUtility.sendemailForNewRfq("to@test.com", internetAddress, javaMailSender, "msg", "type"));
        assertTrue(MailUtility.eamilfornewVendor("to@test.com", internetAddress, javaMailSender, "msg", "type"));

        // emailnewVendor & emailforgotpassword
        assertDoesNotThrow(() -> MailUtility.emailnewVendor("type", "to@test.com", javaMailSender, internetAddress, "V1", "host", user));
        assertDoesNotThrow(() -> MailUtility.emailforgotpassword("type", "to@test.com", javaMailSender, internetAddress, "newpass", "host", user));

        // bad javaMailSender for mailingVerificationLinkWithUserLogin
        JavaMailSender badSender2 = mock(JavaMailSender.class);
        when(badSender2.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("err")).when(badSender2).send(any(MimeMessage.class));
        assertThrows(AppException.class, () -> MailUtility.mailingVerificationLinkWithUserLogin(badSender2, "from@test.com", internetAddress, "pass", "host", user));
        // Extra branch tests for 90%+
        assertDoesNotThrow(() -> MailUtility.emailNewGMTRfqForNoPR("str", "Prefix", javaMailSender, itemRfq, "host", "vendor@test.com", null, "from@test.com", "pass", "2026-12-31", "V100"));

        assertDoesNotThrow(() -> MailUtility.mailingVerificationLinkWithSelfUserLogin(javaMailSender, "from@test.com", internetAddress, "pass", "host", user));

        JavaMailSender failingSender = mock(JavaMailSender.class);
        when(failingSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("send err")).when(failingSender).send(any(MimeMessage.class));

        assertThrows(AppException.class, () -> MailUtility.mailingVerificationLinkWithSelfUserLogin(failingSender, "from@test.com", internetAddress, "pass", "host", user));
        assertThrows(AppException.class, () -> MailUtility.mailingVerificationLinkWithUser(failingSender, internetAddress, "host", user));
    }

    @Test
    void testReachableMissedCollectionAndPincodeBranches() throws Exception {
        MailUtility.emaildailyRFQStatus(
                "type", Collections.singletonList("id1"), javaMailSender,
                internetAddress, null, "http://host");

        ClientDeliveryLocationRfq locationWithoutPincode = new ClientDeliveryLocationRfq();
        locationWithoutPincode.setCity("Bengaluru");
        locationWithoutPincode.setPincode(null);

        RfqItem item = new RfqItem();
        item.setDescription("Item without pincode");
        Rfq rfqWithoutPincode = new Rfq();
        rfqWithoutPincode.setRfqId("RFQ-NO-PIN");
        rfqWithoutPincode.setClientdeliverylocationrfq(
                Collections.singletonList(locationWithoutPincode));
        rfqWithoutPincode.setRfqItem(Collections.singletonList(item));
        rfqWithoutPincode.setRfqDocument(Collections.emptyList());

        assertTrue(MailUtility.emailInviteRfq(
                javaMailSender, rfqWithoutPincode, "http://host", "to@test.com",
                "from@test.com", null, "9876543210", "Full Name",
                "mail2@test.com", "pass2", "9876543210"));
        assertTrue(MailUtility.emailInviteRfqForExistingUsers(
                javaMailSender, rfqWithoutPincode, "http://host", "to@test.com",
                "from@test.com", null, "9876543210", "Full Name",
                "mail2@test.com", "pass2", "9876543210"));
    }
}
