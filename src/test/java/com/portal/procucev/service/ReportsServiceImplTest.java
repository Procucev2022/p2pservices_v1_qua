package com.portal.procucev.service;

import com.portal.procucev.Dto.BuyerCategoryReportDto;
import com.portal.procucev.Dto.SellerCategoryReportDto;
import com.portal.procucev.dao.*;
import com.portal.procucev.model.OrgType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportsServiceImplTest {

    @Mock
    private UserDao userDao;
    @Mock
    private OrgDao orgDao;
    @Mock
    private OrgTypeDao orgTypeDao;
    @Mock
    private GmtRfqVendorDao gmtRfqVendorDao;
    @Mock
    private RfqDao rfqDao;

    @InjectMocks
    private ReportsServiceImpl service;

    @Test
    void testGetSellerReports() throws Exception {
        OrgType orgType = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(orgType);

        when(gmtRfqVendorDao.getSellerReport(any(), any())).thenReturn(Collections.emptyList());
        when(orgDao.getSellerSummary(any(), any(), any())).thenReturn(Collections.emptyList());

        assertNotNull(service.getSellerReports("2026-01-01", "2026-01-31", "sellerReport"));
        assertNotNull(service.getSellerReports("2026-01-01", "2026-01-31", "sellerSummary"));

        assertThrows(RuntimeException.class, () -> service.getSellerReports("2026-01-01", "2026-01-31", "invalid"));
    }

    @Test
    void testGetSellerCategoryReport() throws Exception {
        OrgType orgType = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(orgType);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), "Cat1"});
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), "Cat2"});
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), "Cat3"});
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), "Cat4"});
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), "Cat5"});
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), "Cat6"});
        rows.add(new Object[]{"ORG1", "Company1", "c1@test.com", "9876543210", "Seller1", new Date(), null});
        when(userDao.getSellerCategoryRawData(any(), any())).thenReturn(rows);

        List<?> list = service.getSellerReports("2026-01-01", "2026-01-31", "sellerCategoryReport");
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    void testGetBuyerReports() throws Exception {
        OrgType orgType = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(orgType);

        when(rfqDao.getBuyerReport(any(), any())).thenReturn(Collections.emptyList());
        when(orgDao.getBuyerSummary(any(), any(), any())).thenReturn(Collections.emptyList());

        assertNotNull(service.getBuyerReports("2026-01-01", "2026-01-31", "buyerReport"));
        assertNotNull(service.getBuyerReports("2026-01-01", "2026-01-31", "buyerSummary"));

        assertThrows(RuntimeException.class, () -> service.getBuyerReports("2026-01-01", "2026-01-31", "invalid"));
    }

    @Test
    void testGetBuyerCategoryReport() throws Exception {
        OrgType orgType = new OrgType();
        when(orgTypeDao.findByTypeName(anyString())).thenReturn(orgType);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat1"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat2"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat3"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat4"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat5"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat6"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat7"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat8"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat9"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat10"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), "Cat11"});
        rows.add(new Object[]{"ORG1", "Buyer1", "b1@test.com", "9876543210", "Comp1", "City1", new Date(), null});
        when(userDao.getBuyerCategoryRawData(any(), any())).thenReturn(rows);

        List<?> list = service.getBuyerReports("2026-01-01", "2026-01-31", "buyerCategoryReport");
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    void testGetRfqReports() throws Exception {
        when(rfqDao.getRfqReport(any(), any())).thenReturn(Collections.emptyList());
        when(rfqDao.getRfqSummaryReport(any(), any())).thenReturn(Collections.emptyList());

        assertNotNull(service.getRfqReports("2026-01-01", "2026-01-31", "rfqReport"));
        assertNotNull(service.getRfqReports("2026-01-01", "2026-01-31", "rfqSummaryReport"));

        assertThrows(RuntimeException.class, () -> service.getRfqReports("2026-01-01", "2026-01-31", "invalid"));
    }
}
