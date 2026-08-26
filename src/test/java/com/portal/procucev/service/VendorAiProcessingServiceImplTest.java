package com.portal.procucev.service;

import com.portal.procucev.dao.BuyerVendorAiProfileDao;
import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import com.portal.procucev.rfq.client.GeminiApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VendorAiProcessingServiceImplTest {

    @Mock
    private BuyerVendorAiProfileDao aiProfileDao;

    @Mock
    private BuyerVendorDao buyerVendorDao;

    @Mock
    private GeminiApiClient geminiApiClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private VendorAiProcessingServiceImpl service;

    private BuyerVendor vendorHighQuality;
    private BuyerVendor vendorMediumQuality;
    private BuyerVendor vendorLowQuality;

    @BeforeEach
    void setUp() {
        vendorHighQuality = new BuyerVendor();
        vendorHighQuality.setId("vnd-uuid-high");
        vendorHighQuality.setVendorCode("VND-HIGH");
        vendorHighQuality.setVendorName("High Tech Solutions");
        vendorHighQuality.setGstin("27AAAAA0000A1Z5");
        vendorHighQuality.setPan("AAAAA0000A");
        vendorHighQuality.setPhone1("9876543210");
        vendorHighQuality.setPhone2("9876543211");
        vendorHighQuality.setAddressLine("100 Industrial Area");
        vendorHighQuality.setCity("Mumbai");
        vendorHighQuality.setRegionCode("MH");
        vendorHighQuality.setPostalCode("400001");
        vendorHighQuality.setTypeOfBusiness("Manufacturer");
        vendorHighQuality.setTypeOfIndustry("IT & Software");
        vendorHighQuality.setSearchTerm("HighTech");
        vendorHighQuality.setCountry("India");
        vendorHighQuality.setVendorGroup("Tier 1");
        vendorHighQuality.setSourcingScope("Global");

        vendorMediumQuality = new BuyerVendor();
        vendorMediumQuality.setId("vnd-uuid-med");
        vendorMediumQuality.setVendorCode("VND-MED");
        vendorMediumQuality.setVendorName("Medium Enterprise");
        vendorMediumQuality.setGstin("27BBBBB0000B1Z5");
        vendorMediumQuality.setPan(""); // Empty PAN string
        vendorMediumQuality.setPhone1("9876543212");
        vendorMediumQuality.setAddressLine("200 Road");
        vendorMediumQuality.setCity("Pune");
        vendorMediumQuality.setTypeOfBusiness("");
        vendorMediumQuality.setTypeOfIndustry("");
        vendorMediumQuality.setCountry(null);
        vendorMediumQuality.setVendorGroup(null);
        vendorMediumQuality.setSourcingScope(null);
        vendorMediumQuality.setSearchTerm(null);

        vendorLowQuality = new BuyerVendor();
        vendorLowQuality.setId("vnd-uuid-low");
        vendorLowQuality.setVendorCode("VND-LOW");
        vendorLowQuality.setVendorName("Basic Shop");
        vendorLowQuality.setGstin(null);
        vendorLowQuality.setPan("AAAAA1111A");
        vendorLowQuality.setPhone1("");
        vendorLowQuality.setAddressLine("");
        vendorLowQuality.setCity("");
    }

    @Test
    void testProcessVendorsWithAiEmptyOrNull() {
        assertEquals(0, service.processVendorsWithAi(null, "ORG-1", "user").size());
        assertEquals(0, service.processVendorsWithAi(List.of(), "ORG-1", "user").size());
    }

    @Test
    void testProcessVendorsWithAiSuccessNewAndExistingProfiles() throws Exception {
        String mockGeminiJson = """
            ```json
            {
              "industry": "IT & Software",
              "category": "Cloud Infrastructure",
              "subCategories": ["SaaS", "PaaS", "IaaS"],
              "capabilities": ["High Availability", "24/7 Support"],
              "suitableProcurementCategories": ["Enterprise IT", "Data Center"]
            }
            ```
        """;
        when(geminiApiClient.generateContent(anyString(), anyList(), any())).thenReturn(mockGeminiJson);

        BuyerVendorAiProfile existingProfile = new BuyerVendorAiProfile();
        existingProfile.setVendorCode("VND-HIGH");
        existingProfile.setBuyerOrgId("ORG-1");

        when(aiProfileDao.findByVendorCodeAndBuyerOrgId("VND-HIGH", "ORG-1")).thenReturn(Optional.of(existingProfile));
        when(aiProfileDao.findByVendorCodeAndBuyerOrgId("VND-MED", "ORG-1")).thenReturn(Optional.empty());
        when(aiProfileDao.save(any(BuyerVendorAiProfile.class))).thenAnswer(i -> i.getArgument(0));

        List<BuyerVendorAiProfile> results = service.processVendorsWithAi(
            List.of(vendorHighQuality, vendorMediumQuality), "ORG-1", "admin"
        );

        assertNotNull(results);
        assertEquals(2, results.size());

        BuyerVendorAiProfile profile1 = results.get(0);
        assertEquals("VND-HIGH", profile1.getVendorCode());
        assertEquals("IT & Software", profile1.getIndustry());
        assertEquals("Cloud Infrastructure", profile1.getCategory());
        assertEquals("Qualified", profile1.getQualification());
        assertEquals("100% Provided", profile1.getVerificationStatus());
        assertTrue(profile1.isGstinVerified());
        assertTrue(profile1.isPanVerified());
        assertEquals("admin", profile1.getLastModifiedBy());

        BuyerVendorAiProfile profile2 = results.get(1);
        assertEquals("VND-MED", profile2.getVendorCode());
        assertEquals("Pending", profile2.getQualification());
        assertEquals("Partial Information", profile2.getVerificationStatus());
        assertEquals("Pending Review", profile2.getComplianceStatus());
        assertEquals("admin", profile2.getCreatedBy());
    }

    @Test
    void testProcessVendorsWithAiAllBranchCombinations() throws Exception {
        // High score with only GSTIN (80% Provided)
        BuyerVendor vendorGstinOnly = new BuyerVendor();
        vendorGstinOnly.setVendorCode("VND-GSTIN");
        vendorGstinOnly.setVendorName("GSTIN Only");
        vendorGstinOnly.setGstin("27AAAAA0000A1Z5");
        vendorGstinOnly.setPan(null);
        vendorGstinOnly.setPhone1("9876543210");
        vendorGstinOnly.setAddressLine("Line 1");
        vendorGstinOnly.setCity("City 1");
        vendorGstinOnly.setTypeOfBusiness("Supplier");
        vendorGstinOnly.setTypeOfIndustry("Mfg");

        // High score with only PAN (80% Provided)
        BuyerVendor vendorPanOnly = new BuyerVendor();
        vendorPanOnly.setVendorCode("VND-PAN");
        vendorPanOnly.setVendorName("PAN Only");
        vendorPanOnly.setGstin(null);
        vendorPanOnly.setPan("AAAAA0000A");
        vendorPanOnly.setPhone1("9876543210");
        vendorPanOnly.setAddressLine("Line 1");
        vendorPanOnly.setCity("City 1");
        vendorPanOnly.setTypeOfBusiness("Supplier");
        vendorPanOnly.setTypeOfIndustry("Mfg");

        // Address only, no city
        BuyerVendor vendorAddressOnly = new BuyerVendor();
        vendorAddressOnly.setVendorCode("VND-ADDR");
        vendorAddressOnly.setVendorName("Address Only");
        vendorAddressOnly.setAddressLine("Road");
        vendorAddressOnly.setCity(null);

        // City only, no address
        BuyerVendor vendorCityOnly = new BuyerVendor();
        vendorCityOnly.setVendorCode("VND-CITY");
        vendorCityOnly.setVendorName("City Only");
        vendorCityOnly.setAddressLine(null);
        vendorCityOnly.setCity("Mumbai");

        // Vendor with low score < 70 (neither GSTIN nor PAN)
        BuyerVendor vendorBare = new BuyerVendor();
        vendorBare.setVendorCode("VND-BARE");
        vendorBare.setVendorName("Bare Vendor");
        vendorBare.setGstin(null);
        vendorBare.setPan(null);
        vendorBare.setPhone1(null);
        vendorBare.setAddressLine(null);
        vendorBare.setCity(null);

        // Vendor with whitespace-only fields
        BuyerVendor vendorBlanks = new BuyerVendor();
        vendorBlanks.setVendorCode("VND-BLANK");
        vendorBlanks.setVendorName("Blank Fields Vendor");
        vendorBlanks.setGstin("   ");
        vendorBlanks.setPan("   ");
        vendorBlanks.setPhone1("   ");
        vendorBlanks.setAddressLine("   ");
        vendorBlanks.setCity("   ");
        vendorBlanks.setTypeOfBusiness("   ");
        vendorBlanks.setTypeOfIndustry("   ");

        String rawJson = "{\"industry\": \"General\", \"category\": \"Goods\"}";
        when(geminiApiClient.generateContent(anyString(), anyList(), any())).thenReturn(rawJson);
        when(aiProfileDao.findByVendorCodeAndBuyerOrgId(anyString(), anyString())).thenReturn(Optional.empty());
        when(aiProfileDao.save(any(BuyerVendorAiProfile.class))).thenAnswer(i -> i.getArgument(0));

        List<BuyerVendorAiProfile> results = service.processVendorsWithAi(
            List.of(vendorGstinOnly, vendorPanOnly, vendorAddressOnly, vendorCityOnly, vendorBare, vendorBlanks, vendorLowQuality),
            "ORG-1", "admin"
        );

        assertEquals(7, results.size());
        assertEquals("80% Provided", results.get(0).getVerificationStatus());
        assertEquals("Qualified", results.get(0).getQualification());
        assertEquals(75, results.get(0).getComplianceScore());

        assertEquals("Partial Information", results.get(1).getVerificationStatus());
        assertEquals("Pending", results.get(1).getQualification());
        assertEquals(75, results.get(1).getComplianceScore());

        assertFalse(results.get(2).isCompanyInfoVerified());
        assertFalse(results.get(3).isCompanyInfoVerified());

        assertEquals("Unqualified", results.get(4).getQualification());
        assertEquals("Incomplete", results.get(4).getVerificationStatus());
        assertEquals("Non-Compliant", results.get(4).getComplianceStatus());
        assertEquals(60, results.get(4).getComplianceScore());

        assertEquals("Unqualified", results.get(5).getQualification());
    }

    @Test
    void testProcessVendorsWithAiGeminiExceptionThrows() throws Exception {
        when(geminiApiClient.generateContent(anyString(), anyList(), any())).thenThrow(new RuntimeException("Gemini API Rate Limit"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            service.processVendorsWithAi(List.of(vendorHighQuality), "ORG-1", "admin")
        );
        assertTrue(ex.getMessage().contains("Gemini AI execution failed"));
    }

    @Test
    void testGetAnalyzedVendors() {
        BuyerVendorAiProfile p = new BuyerVendorAiProfile();
        p.setVendorCode("VND-001");
        when(aiProfileDao.findByBuyerOrgIdOrderByCreatedTSDesc("ORG-1")).thenReturn(List.of(p));

        List<BuyerVendorAiProfile> list = service.getAnalyzedVendors("ORG-1");
        assertEquals(1, list.size());
        assertEquals("VND-001", list.get(0).getVendorCode());
    }

    @Test
    void testGetVendorAiProfileFoundInAiTable() {
        BuyerVendorAiProfile p = new BuyerVendorAiProfile();
        p.setVendorCode("VND-001");
        when(aiProfileDao.findByVendorCodeAndBuyerOrgId("VND-001", "ORG-1")).thenReturn(Optional.of(p));

        Optional<BuyerVendorAiProfile> res = service.getVendorAiProfile("VND-001", "ORG-1");
        assertTrue(res.isPresent());
        assertEquals("VND-001", res.get().getVendorCode());
    }

    @Test
    void testGetVendorAiProfileFoundInMasterTableAndAnalyzed() throws Exception {
        when(aiProfileDao.findByVendorCodeAndBuyerOrgId("VND-HIGH", "ORG-1")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("VND-HIGH", "ORG-1")).thenReturn(Optional.of(vendorHighQuality));

        String mockGeminiJson = """
            ```
            {
              "industry": "IT & Software",
              "category": "Cloud Infrastructure",
              "subCategories": ["SaaS"],
              "capabilities": ["24/7"],
              "suitableProcurementCategories": ["IT"]
            }
            ```
        """;
        when(geminiApiClient.generateContent(anyString(), anyList(), any())).thenReturn(mockGeminiJson);
        when(aiProfileDao.save(any(BuyerVendorAiProfile.class))).thenAnswer(i -> i.getArgument(0));

        Optional<BuyerVendorAiProfile> res = service.getVendorAiProfile("VND-HIGH", "ORG-1");
        assertTrue(res.isPresent());
        assertEquals("VND-HIGH", res.get().getVendorCode());
        assertEquals("IT & Software", res.get().getIndustry());
    }

    @Test
    void testGetVendorAiProfileNotFound() {
        when(aiProfileDao.findByVendorCodeAndBuyerOrgId("UNKNOWN", "ORG-1")).thenReturn(Optional.empty());
        when(buyerVendorDao.findByVendorCodeAndBuyerOrgId("UNKNOWN", "ORG-1")).thenReturn(Optional.empty());

        Optional<BuyerVendorAiProfile> res = service.getVendorAiProfile("UNKNOWN", "ORG-1");
        assertTrue(res.isEmpty());
    }

    @Test
    void testSanitizeJsonAllBranches() {
        assertEquals("{}", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", (Object) null));
        assertEquals("{\"a\":1}", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "```json\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "```\n{\"a\":1}\n```"));
        assertEquals("{\"a\":1}", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "prefix {\"a\":1} suffix"));
        assertEquals("not json", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "not json"));
        assertEquals("{only open", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "{only open"));
        assertEquals("only close}", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "only close}"));
        assertEquals("}{reversed", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "}{reversed"));
        assertEquals("raw without backtick", ReflectionTestUtils.invokeMethod(service, "sanitizeJson", "raw without backtick"));
    }

    @Test
    void testEnsureTableExistsExceptionHandled() {
        doThrow(new RuntimeException("DB error")).when(jdbcTemplate).execute(anyString());
        when(aiProfileDao.findByBuyerOrgIdOrderByCreatedTSDesc(anyString())).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getAnalyzedVendors("ORG-1"));
    }
}
