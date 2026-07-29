package com.portal.procucev.service;

import com.portal.procucev.dao.BFSDao;
import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.dao.OrgDao;
import com.portal.procucev.dao.VendorCatalogueDao;
import com.portal.procucev.dao.VendorTermsConditionsDao;
import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.model.VendorCatalogue;
import com.portal.procucev.model.VendorTermsConditions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorCatalogueServiceImplTest {

    @Mock
    private VendorCatalogueDao vendorCatalogueDao;
    @Mock
    private VendorTermsConditionsDao termsDao;
    @Mock
    private BFSDao bfsDao;
    @Mock
    private MasterStatusDao masterStatusDao;
    @Mock
    private OrgDao orgDao;

    @InjectMocks
    private VendorCatalogueServiceImpl service;

    @Test
    void testSaveCatalogue_Null() {
        assertThrows(IllegalArgumentException.class, () -> service.saveCatalogue(null));
    }

    @Test
    void testSaveCatalogue_Success() {
        VendorCatalogue catalogue = new VendorCatalogue();
        catalogue.setMaterialDescription("Steel");
        catalogue.setUom("KG");
        catalogue.setMinOrderQuantity(100);
        catalogue.setAvailableQuantity(500);
        catalogue.setPricePerUom(BigDecimal.valueOf(50));

        when(vendorCatalogueDao.save(catalogue)).thenReturn(catalogue);
        when(masterStatusDao.findByStatus(any())).thenReturn(new MasterStatus());
        when(orgDao.getCityByOrg(any())).thenReturn("Mumbai");

        VendorCatalogue saved = service.saveCatalogue(catalogue);
        assertNotNull(saved);
        verify(bfsDao).save(any(BFSItems.class));
    }

    @Test
    void testGetCataloguesByVendorId() {
        assertTrue(service.getCataloguesByVendorId(null).isEmpty());

        when(vendorCatalogueDao.findCatalogueByVendor("V1")).thenReturn(Collections.emptyList());
        assertTrue(service.getCataloguesByVendorId("V1").isEmpty());

        VendorCatalogue cat = new VendorCatalogue();
        when(vendorCatalogueDao.findCatalogueByVendor("V2")).thenReturn(List.of(cat));
        assertEquals(1, service.getCataloguesByVendorId("V2").size());
    }

    @Test
    void testSaveTermsAndConditions() {
        assertThrows(IllegalArgumentException.class, () -> service.saveTermsAndConditions(null));

        VendorTermsConditions tc = new VendorTermsConditions();
        when(termsDao.save(tc)).thenReturn(tc);

        VendorTermsConditions saved = service.saveTermsAndConditions(tc);
        assertNotNull(saved);
    }

    @Test
    void testGetTCByVendorId() {
        assertTrue(service.getTCByVendorId(null).isEmpty());

        when(termsDao.findByOrgIdOrderByCreatedTSDesc("V1")).thenReturn(Collections.emptyList());
        assertTrue(service.getTCByVendorId("V1").isEmpty());

        VendorTermsConditions tc = new VendorTermsConditions();
        when(termsDao.findByOrgIdOrderByCreatedTSDesc("V2")).thenReturn(List.of(tc));
        assertEquals(1, service.getTCByVendorId("V2").size());
    }
}
