package com.portal.procucev.service;

import com.portal.procucev.dao.BuyerVendorAiProfileDao;
import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BuyerVendorServiceImplTest {

    @Mock
    private BuyerVendorDao buyerVendorDao;

    @Mock
    private BuyerVendorAiProfileDao buyerVendorAiProfileDao;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BuyerVendorServiceImpl service;

    private BuyerVendor sampleVendor;
    private BuyerVendorAiProfile sampleAiProfile;

    @BeforeEach
    void setUp() {
        sampleVendor = new BuyerVendor();
        sampleVendor.setId("uuid-123");
        sampleVendor.setVendorCode("VND-001");
        sampleVendor.setVendorName("Acme Corporation");
        sampleVendor.setBuyerOrgId("ORG-999");
        sampleVendor.setPhone1("9876543210");
        sampleVendor.setStatus("Active");
        sampleVendor.setSourcingScope("Client Only");
        sampleVendor.setCountry("IN");
        sampleVendor.setCity("Mumbai");
        sampleVendor.setTypeOfBusiness("Manufacturer");
        sampleVendor.setTypeOfIndustry("Industrial");

        sampleAiProfile = new BuyerVendorAiProfile();
        sampleAiProfile.setId("ai-uuid-123");
        sampleAiProfile.setVendorCode("VND-001");
        sampleAiProfile.setVendorName("Acme Corporation");
        sampleAiProfile.setBuyerOrgId("ORG-999");
    }

    @Test
    void testGetVendorsWithAllFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BuyerVendor> page = new PageImpl<>(List.of(sampleVendor));
        when(buyerVendorDao.findByBuyerOrgFiltered("ORG-999", "Active", "Industrial", "Acme", pageable)).thenReturn(page);

        Page<BuyerVendor> result = service.getVendors("ORG-999", "Active", "Industrial", "Acme", pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void testGetVendorsWithNullOrEmptyFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BuyerVendor> page = new PageImpl<>(List.of(sampleVendor));
        when(buyerVendorDao.findByBuyerOrgFiltered("ORG-999", null, null, null, pageable)).thenReturn(page);

        Page<BuyerVendor> result = service.getVendors("ORG-999", "", "", "", pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        Page<BuyerVendor> resultNull = service.getVendors("ORG-999", null, null, null, pageable);
        assertNotNull(resultNull);
    }

    @Test
    void testCreateVendorSuccess() {
        when(buyerVendorDao.existsByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(false);
        when(buyerVendorDao.save(sampleVendor)).thenReturn(sampleVendor);

        BuyerVendor created = service.createVendor(sampleVendor);
        assertNotNull(created);
        assertEquals("VND-001", created.getVendorCode());
    }

    @Test
    void testCreateVendorDuplicateThrowsException() {
        when(buyerVendorDao.existsByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createVendor(sampleVendor));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testGetVendorByIdPrimaryUuid() {
        when(buyerVendorDao.findByIdAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.of(sampleVendor));

        Optional<BuyerVendor> result = service.getVendorById("uuid-123", "ORG-999");
        assertTrue(result.isPresent());
        assertEquals("uuid-123", result.get().getId());
    }

    @Test
    void testGetVendorByIdFallbackVendorCode() {
        when(buyerVendorDao.findByIdAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.of(sampleVendor));

        Optional<BuyerVendor> result = service.getVendorById("VND-001", "ORG-999");
        assertTrue(result.isPresent());
        assertEquals("VND-001", result.get().getVendorCode());
    }

    @Test
    void testGetVendorByIdNotFound() {
        when(buyerVendorDao.findByIdAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());

        Optional<BuyerVendor> result = service.getVendorById("unknown", "ORG-999");
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateVendorSuccess() {
        BuyerVendor updatedData = new BuyerVendor();
        updatedData.setVendorCode("VND-001-NEW");
        updatedData.setVendorName("Acme Updated");
        updatedData.setSearchTerm("AcmeSearch");
        updatedData.setPan("ABCDE1234F");
        updatedData.setGstin("27ABCDE1234F1Z5");
        updatedData.setCountry("IN");
        updatedData.setRegionCode("MH");
        updatedData.setAddressLine("123 Main St");
        updatedData.setCity("Pune");
        updatedData.setDistrict("Pune");
        updatedData.setPostalCode("411001");
        updatedData.setPhone1("9876543210");
        updatedData.setPhone2("9876543211");
        updatedData.setTypeOfBusiness("Trading");
        updatedData.setTypeOfIndustry("Metals");
        updatedData.setVendorGroup("Group A");
        updatedData.setSourcingScope("Global");

        when(buyerVendorDao.findByIdAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.of(sampleVendor));
        when(buyerVendorDao.save(any(BuyerVendor.class))).thenAnswer(i -> i.getArgument(0));

        BuyerVendor result = service.updateVendor("uuid-123", updatedData, "ORG-999");
        assertNotNull(result);
        assertEquals("Acme Updated", result.getVendorName());
        assertEquals("Pune", result.getCity());
        assertEquals("Global", result.getSourcingScope());
    }

    @Test
    void testUpdateVendorNotFound() {
        when(buyerVendorDao.findByIdAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.updateVendor("unknown", sampleVendor, "ORG-999"));
    }

    @Test
    void testUpdateStatusSuccess() {
        when(buyerVendorDao.findByIdAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.of(sampleVendor));
        when(buyerVendorDao.save(any(BuyerVendor.class))).thenReturn(sampleVendor);

        boolean updated = service.updateStatus("uuid-123", "ORG-999", "Inactive");
        assertTrue(updated);
        assertEquals("Inactive", sampleVendor.getStatus());
    }

    @Test
    void testUpdateStatusNotFound() {
        when(buyerVendorDao.findByIdAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());

        boolean updated = service.updateStatus("unknown", "ORG-999", "Inactive");
        assertFalse(updated);
    }

    @Test
    void testDeleteVendorNullOrBlank() {
        assertFalse(service.deleteVendor(null, "ORG-999"));
        assertFalse(service.deleteVendor("  ", "ORG-999"));
    }

    @Test
    void testDeleteVendorByAiIdAndMasterId() {
        when(buyerVendorAiProfileDao.findById("uuid-123")).thenReturn(Optional.of(sampleAiProfile));
        when(buyerVendorDao.findByIdAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.of(sampleVendor));
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.of(sampleVendor));
        doThrow(new RuntimeException("Ignore")).when(buyerVendorAiProfileDao).deleteByVendorCodeAndBuyerOrgId("VND-001", "ORG-999");

        boolean deleted = service.deleteVendor("uuid-123", "ORG-999");
        assertTrue(deleted);
        verify(buyerVendorAiProfileDao).delete(sampleAiProfile);
        verify(buyerVendorDao, atLeastOnce()).delete(sampleVendor);
    }

    @Test
    void testDeleteVendorByAiIdWithNullOrg() {
        sampleAiProfile.setBuyerOrgId(null);
        when(buyerVendorAiProfileDao.findById("uuid-123")).thenReturn(Optional.of(sampleAiProfile));
        when(buyerVendorDao.findByIdAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.empty());

        boolean deleted = service.deleteVendor("uuid-123", "ORG-999");
        assertTrue(deleted);
    }

    @Test
    void testDeleteVendorByAiIdWithDifferentOrgFallback() {
        sampleAiProfile.setBuyerOrgId("DIFFERENT_ORG");
        when(buyerVendorAiProfileDao.findById("uuid-123")).thenReturn(Optional.of(sampleAiProfile));
        when(buyerVendorAiProfileDao.findByVendorCodeAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByIdAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("uuid-123", "ORG-999")).thenReturn(Optional.empty());

        boolean deleted = service.deleteVendor("uuid-123", "ORG-999");
        assertFalse(deleted);
    }

    @Test
    void testDeleteVendorByAiCodeAndMasterCode() {
        when(buyerVendorAiProfileDao.findById("VND-001")).thenReturn(Optional.empty());
        when(buyerVendorAiProfileDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.of(sampleAiProfile));
        when(buyerVendorDao.findByIdAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.of(sampleVendor));

        boolean deleted = service.deleteVendor("VND-001", "ORG-999");
        assertTrue(deleted);
        verify(buyerVendorAiProfileDao).delete(sampleAiProfile);
        verify(buyerVendorDao).delete(sampleVendor);
    }

    @Test
    void testDeleteVendorNotFound() {
        when(buyerVendorAiProfileDao.findById("unknown")).thenReturn(Optional.empty());
        when(buyerVendorAiProfileDao.findByVendorCodeAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByIdAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("unknown", "ORG-999")).thenReturn(Optional.empty());

        boolean deleted = service.deleteVendor("unknown", "ORG-999");
        assertFalse(deleted);
    }

    @Test
    void testBulkDeleteVendors() {
        assertEquals(0, service.bulkDeleteVendors(null, "ORG-999"));
        assertEquals(0, service.bulkDeleteVendors(List.of(), "ORG-999"));

        when(buyerVendorAiProfileDao.findById(anyString())).thenReturn(Optional.empty());
        when(buyerVendorAiProfileDao.findByVendorCodeAndBuyerOrgId(anyString(), eq("ORG-999"))).thenReturn(Optional.empty());
        when(buyerVendorDao.findByIdAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.of(sampleVendor));
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-999")).thenReturn(Optional.of(sampleVendor));
        when(buyerVendorDao.findByIdAndBuyerOrgId("VND-002", "ORG-999")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-002", "ORG-999")).thenReturn(Optional.empty());

        int deleted = service.bulkDeleteVendors(Arrays.asList("VND-001", "  ", null, "VND-002"), "ORG-999");
        assertEquals(1, deleted);
    }

    @Test
    void testBulkCreateVendorsEmptyOrNull() {
        Map<String, Object> resNull = service.bulkCreateVendors(null, "ORG-999", "admin");
        assertEquals(0, resNull.get("savedCount"));

        Map<String, Object> resEmpty = service.bulkCreateVendors(List.of(), "ORG-999", "admin");
        assertEquals(0, resEmpty.get("savedCount"));
    }

    @Test
    void testBulkCreateVendorsValidationAndDuplicates() {
        BuyerVendor vMissingCode = new BuyerVendor();
        vMissingCode.setVendorName("Missing Code");

        BuyerVendor vBlankCode = new BuyerVendor();
        vBlankCode.setVendorCode("   ");
        vBlankCode.setVendorName("Blank Code");

        BuyerVendor vNullName = new BuyerVendor();
        vNullName.setVendorCode("VND-001B");
        vNullName.setVendorName(null);

        BuyerVendor vShortName = new BuyerVendor();
        vShortName.setVendorCode("VND-002");
        vShortName.setVendorName("AB");

        BuyerVendor vNullPhone = new BuyerVendor();
        vNullPhone.setVendorCode("VND-002B");
        vNullPhone.setVendorName("Valid Name");
        vNullPhone.setPhone1(null);

        BuyerVendor vInvalidPhone = new BuyerVendor();
        vInvalidPhone.setVendorCode("VND-003");
        vInvalidPhone.setVendorName("Valid Name");
        vInvalidPhone.setPhone1("123");

        BuyerVendor vDup1 = new BuyerVendor();
        vDup1.setVendorCode("VND-004");
        vDup1.setVendorName("Vendor Dup");
        vDup1.setPhone1("9876543210");
        vDup1.setStatus("Pending");
        vDup1.setSourcingScope("Global");
        vDup1.setCountry("USA");

        BuyerVendor vDup2 = new BuyerVendor();
        vDup2.setVendorCode("vnd-004");
        vDup2.setVendorName("Vendor Dup 2");
        vDup2.setPhone1("9876543210");

        BuyerVendor vExistsInDb = new BuyerVendor();
        vExistsInDb.setVendorCode("VND-005");
        vExistsInDb.setVendorName("Vendor Exists In DB");
        vExistsInDb.setPhone1("9876543210");

        BuyerVendor vValid = new BuyerVendor();
        vValid.setVendorCode("VND-006");
        vValid.setVendorName("Vendor Valid");
        vValid.setPhone1("9876543210");
        vValid.setStatus(null);
        vValid.setSourcingScope(null);
        vValid.setCountry(null);

        BuyerVendor vExistsException = new BuyerVendor();
        vExistsException.setVendorCode("VND-007");
        vExistsException.setVendorName("Vendor Exists Exception");
        vExistsException.setPhone1("9876543210");

        when(buyerVendorDao.existsByVendorCodeAndBuyerOrgId("VND-004", "ORG-999")).thenReturn(false);
        when(buyerVendorDao.existsByVendorCodeAndBuyerOrgId("VND-005", "ORG-999")).thenReturn(true);
        when(buyerVendorDao.existsByVendorCodeAndBuyerOrgId("VND-006", "ORG-999")).thenReturn(false);
        when(buyerVendorDao.existsByVendorCodeAndBuyerOrgId("VND-007", "ORG-999")).thenThrow(new RuntimeException("DB temporary error"));

        List<BuyerVendor> list = List.of(
            vMissingCode, vBlankCode, vNullName, vShortName, vNullPhone, vInvalidPhone, vDup1, vDup2, vExistsInDb, vValid, vExistsException
        );

        Map<String, Object> result = service.bulkCreateVendors(list, "ORG-999", "admin");
        assertNotNull(result);
        assertEquals(3, result.get("savedCount")); // vDup1, vValid, vExistsException
        assertEquals(2, result.get("skippedCount")); // vDup2 (batch dup), vExistsInDb (db dup)
        assertEquals(11, result.get("totalCount"));
        verify(buyerVendorDao).saveAll(anyList());

        assertEquals("Active", vValid.getStatus());
        assertEquals("Client Only", vValid.getSourcingScope());
        assertEquals("IN", vValid.getCountry());
        assertEquals("Pending", vDup1.getStatus());
        assertEquals("Global", vDup1.getSourcingScope());
        assertEquals("USA", vDup1.getCountry());
    }

    @Test
    void testEnsureTableExistsAlterExceptionHandled() {
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("ALTER TABLE")) {
                throw new DataAccessException("Alter error") {};
            }
            return null;
        }).when(jdbcTemplate).execute(anyString());

        ReflectionTestUtils.invokeMethod(service, "ensureTableExists");
    }

    @Test
    void testEnsureTableExistsExceptionHandled() {
        doThrow(new RuntimeException("DB Connection failed")).when(jdbcTemplate).execute(anyString());
        Pageable pageable = PageRequest.of(0, 10);
        when(buyerVendorDao.findByBuyerOrgFiltered(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        assertDoesNotThrow(() -> service.getVendors("ORG-999", null, null, null, pageable));
    }
}
