package com.portal.procucev.controller;

import com.portal.procucev.customexception.MessageResponse;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.VendorCatalogue;
import com.portal.procucev.model.VendorTermsConditions;
import com.portal.procucev.service.VendorCatalogueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorCatalogueControllerTest {

    @Mock
    private VendorCatalogueService catalogueService;

    @InjectMocks
    private VendorCatalogueController controller;

    @Test
    void testSaveCatalogue_CreateSuccess() {
        VendorCatalogue cat = new VendorCatalogue();
        cat.setId(null);
        VendorCatalogue saved = new VendorCatalogue();
        saved.setId("CAT1");
        when(catalogueService.saveCatalogue(cat)).thenReturn(saved);

        ResponseEntity<?> resp = controller.saveCatalogue(cat);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testSaveCatalogue_UpdateSuccess() {
        VendorCatalogue cat = new VendorCatalogue();
        cat.setId("CAT1");
        when(catalogueService.saveCatalogue(cat)).thenReturn(cat);

        ResponseEntity<?> resp = controller.saveCatalogue(cat);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testSaveCatalogue_ValidationFailure() {
        VendorCatalogue cat = new VendorCatalogue();
        when(catalogueService.saveCatalogue(cat)).thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<?> resp = controller.saveCatalogue(cat);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void testSaveCatalogue_SystemError() {
        VendorCatalogue cat = new VendorCatalogue();
        when(catalogueService.saveCatalogue(cat)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> resp = controller.saveCatalogue(cat);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void testGetCataloguesByVendorId_Found() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getCataloguesByVendorId("ORG1")).thenReturn(List.of(new VendorCatalogue()));

        ResponseEntity<MessageResponse> resp = controller.getCataloguesByVendorId(org);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testGetCataloguesByVendorId_NotFound() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getCataloguesByVendorId("ORG1")).thenReturn(Collections.emptyList());

        ResponseEntity<MessageResponse> resp = controller.getCataloguesByVendorId(org);
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void testGetCataloguesByVendorId_ValidationFailed() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getCataloguesByVendorId("ORG1")).thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<MessageResponse> resp = controller.getCataloguesByVendorId(org);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void testGetCataloguesByVendorId_SystemError() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getCataloguesByVendorId("ORG1")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> resp = controller.getCataloguesByVendorId(org);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void testSaveTermsAndConditions_Success() {
        VendorTermsConditions tc = new VendorTermsConditions();
        tc.setId("TC1");
        when(catalogueService.saveTermsAndConditions(tc)).thenReturn(tc);

        ResponseEntity<?> resp = controller.saveTermsAndConditions(tc);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testSaveTermsAndConditions_ValidationFailed() {
        VendorTermsConditions tc = new VendorTermsConditions();
        when(catalogueService.saveTermsAndConditions(tc)).thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<?> resp = controller.saveTermsAndConditions(tc);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void testSaveTermsAndConditions_SystemError() {
        VendorTermsConditions tc = new VendorTermsConditions();
        when(catalogueService.saveTermsAndConditions(tc)).thenThrow(new RuntimeException("Error"));

        ResponseEntity<?> resp = controller.saveTermsAndConditions(tc);
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    void testGetSellerTC_Found() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getTCByVendorId("ORG1")).thenReturn(List.of(new VendorTermsConditions()));

        ResponseEntity<MessageResponse> resp = controller.getSellerTC(org);
        assertEquals(200, resp.getStatusCode().value());
    }

    @Test
    void testGetSellerTC_NotFound() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getTCByVendorId("ORG1")).thenReturn(Collections.emptyList());

        ResponseEntity<MessageResponse> resp = controller.getSellerTC(org);
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void testGetSellerTC_ValidationFailed() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getTCByVendorId("ORG1")).thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<MessageResponse> resp = controller.getSellerTC(org);
        assertEquals(400, resp.getStatusCode().value());
    }

    @Test
    void testGetSellerTC_SystemError() {
        Organization org = new Organization();
        org.setId("ORG1");
        when(catalogueService.getTCByVendorId("ORG1")).thenThrow(new RuntimeException("Error"));

        ResponseEntity<MessageResponse> resp = controller.getSellerTC(org);
        assertEquals(500, resp.getStatusCode().value());
    }
}
