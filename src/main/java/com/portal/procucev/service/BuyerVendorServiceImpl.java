package com.portal.procucev.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.portal.procucev.dao.BuyerVendorAiProfileDao;
import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.model.BuyerVendor;
import com.portal.procucev.model.BuyerVendorAiProfile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class BuyerVendorServiceImpl implements BuyerVendorService {

    private static final Logger log = LoggerFactory.getLogger(BuyerVendorServiceImpl.class);

    @Autowired
    private BuyerVendorDao buyerVendorDao;

    @Autowired
    private BuyerVendorAiProfileDao buyerVendorAiProfileDao;

    @Override
    public Page<BuyerVendor> getVendors(String buyerOrgId, String status, String industry, String search, Pageable pageable) {
        String statusParam = (status != null && !status.isEmpty()) ? status : null;
        String industryParam = (industry != null && !industry.isEmpty()) ? industry : null;
        String searchParam = (search != null && !search.isEmpty()) ? search : null;
        return buyerVendorDao.findByBuyerOrgFiltered(buyerOrgId, statusParam, industryParam, searchParam, pageable);
    }

    @Override
    public BuyerVendor createVendor(BuyerVendor vendor) {
        if (buyerVendorDao.existsByVendorCodeAndBuyerOrgId(vendor.getVendorCode(), vendor.getBuyerOrgId())) {
            throw new IllegalArgumentException("Vendor code '" + vendor.getVendorCode() + "' already exists for this organization");
        }
        return buyerVendorDao.save(vendor);
    }

    @Override
    public Optional<BuyerVendor> getVendorById(String idOrCode, String buyerOrgId) {
        // First try by database UUID primary key
        Optional<BuyerVendor> byId = buyerVendorDao.findByIdAndBuyerOrgId(idOrCode, buyerOrgId);
        if (byId.isPresent()) {
            return byId;
        }
        // Fallback search by vendor code (e.g. VND-001)
        return buyerVendorDao.findByVendorCodeAndBuyerOrgId(idOrCode, buyerOrgId);
    }

    @Override
    public BuyerVendor updateVendor(String idOrCode, BuyerVendor vendor, String buyerOrgId) {
        Optional<BuyerVendor> existing = getVendorById(idOrCode, buyerOrgId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found");
        }
        BuyerVendor entity = existing.get();
        String requestedCode = vendor.getVendorCode();
        if (!Objects.equals(entity.getVendorCode(), requestedCode)) {
            Optional<BuyerVendor> codeOwner = buyerVendorDao.findByVendorCodeAndBuyerOrgId(requestedCode, buyerOrgId);
            if (codeOwner.isPresent() && !Objects.equals(codeOwner.get().getId(), entity.getId())) {
                throw new IllegalArgumentException("Vendor code '" + requestedCode + "' already exists for this organization");
            }
        }
        entity.setVendorCode(requestedCode);
        entity.setVendorName(vendor.getVendorName());
        entity.setSearchTerm(vendor.getSearchTerm());
        entity.setPan(vendor.getPan());
        entity.setGstin(vendor.getGstin());
        entity.setCountry(vendor.getCountry());
        entity.setRegionCode(vendor.getRegionCode());
        entity.setAddressLine(vendor.getAddressLine());
        entity.setCity(vendor.getCity());
        entity.setDistrict(vendor.getDistrict());
        entity.setPostalCode(vendor.getPostalCode());
        entity.setPhone1(vendor.getPhone1());
        entity.setPhone2(vendor.getPhone2());
        entity.setTypeOfBusiness(vendor.getTypeOfBusiness());
        entity.setTypeOfIndustry(vendor.getTypeOfIndustry());
        entity.setVendorGroup(vendor.getVendorGroup());
        entity.setSourcingScope(vendor.getSourcingScope());
        return buyerVendorDao.save(entity);
    }

    @Override
    public boolean updateStatus(String idOrCode, String buyerOrgId, String status) {
        Optional<BuyerVendor> vendorOpt = getVendorById(idOrCode, buyerOrgId);
        if (vendorOpt.isPresent()) {
            BuyerVendor v = vendorOpt.get();
            v.setStatus(status);
            buyerVendorDao.save(v);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteVendor(String idOrCode, String buyerOrgId) {
        if (idOrCode == null || idOrCode.trim().isEmpty()) {
            return false;
        }
        String key = idOrCode.trim();
        boolean deletedAny = false;
        String resolvedVendorCode = null;

        // 1. Try finding AI Profile by primary key UUID or vendorCode
        Optional<BuyerVendorAiProfile> aiById = buyerVendorAiProfileDao.findById(key);
        if (aiById.isPresent() && (aiById.get().getBuyerOrgId() == null || buyerOrgId.equals(aiById.get().getBuyerOrgId()))) {
            resolvedVendorCode = aiById.get().getVendorCode();
            log.info("Deleting AI profile by id: {}, code: {} for buyerOrgId: {}", key, resolvedVendorCode, buyerOrgId);
            buyerVendorAiProfileDao.delete(aiById.get());
            deletedAny = true;
        } else {
            Optional<BuyerVendorAiProfile> aiByCode = buyerVendorAiProfileDao.findByVendorCodeAndBuyerOrgId(key, buyerOrgId);
            if (aiByCode.isPresent()) {
                resolvedVendorCode = aiByCode.get().getVendorCode();
                log.info("Deleting AI profile by vendorCode: {} for buyerOrgId: {}", resolvedVendorCode, buyerOrgId);
                buyerVendorAiProfileDao.delete(aiByCode.get());
                deletedAny = true;
            }
        }

        // 2. Try finding Master BuyerVendor by primary key UUID or resolved vendorCode or key
        Optional<BuyerVendor> masterById = buyerVendorDao.findByIdAndBuyerOrgId(key, buyerOrgId);
        if (masterById.isPresent()) {
            String code = masterById.get().getVendorCode();
            log.info("Deleting master buyer vendor by id: {}, code: {} for buyerOrgId: {}", key, code, buyerOrgId);
            buyerVendorDao.delete(masterById.get());
            deletedAny = true;
            if (resolvedVendorCode == null) {
                resolvedVendorCode = code;
            }
        }

        String codeToLookup = resolvedVendorCode != null ? resolvedVendorCode : key;
        Optional<BuyerVendor> masterByCode = buyerVendorDao.findByVendorCodeAndBuyerOrgId(codeToLookup, buyerOrgId);
        if (masterByCode.isPresent()) {
            log.info("Deleting master buyer vendor by code: {} for buyerOrgId: {}", codeToLookup, buyerOrgId);
            buyerVendorDao.delete(masterByCode.get());
            deletedAny = true;
        }

        // 3. Final safety cleanup of AI profile if resolvedVendorCode was discovered from master record
        if (resolvedVendorCode != null) {
            try {
                buyerVendorAiProfileDao.deleteByVendorCodeAndBuyerOrgId(resolvedVendorCode, buyerOrgId);
            } catch (Exception ignored) {}
        }

        return deletedAny;
    }

    @Override
    @Transactional
    public int bulkDeleteVendors(java.util.List<String> idsOrCodes, String buyerOrgId) {
        if (idsOrCodes == null || idsOrCodes.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;
        for (String idOrCode : idsOrCodes) {
            if (idOrCode != null && !idOrCode.trim().isEmpty()) {
                boolean deleted = deleteVendor(idOrCode.trim(), buyerOrgId);
                if (deleted) {
                    deletedCount++;
                }
            }
        }
        log.info("Bulk deleted {} vendors for buyerOrgId: {}", deletedCount, buyerOrgId);
        return deletedCount;
    }

    @Override

    public java.util.Map<String, Object> bulkCreateVendors(java.util.List<BuyerVendor> vendors, String buyerOrgId, String createdBy) {
        java.util.List<BuyerVendor> toSave = new java.util.ArrayList<>();
        java.util.List<String> skippedCodes = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.Set<String> seenCodes = new java.util.HashSet<>();

        if (vendors == null || vendors.isEmpty()) {
            return java.util.Map.of(
                "savedCount", 0,
                "skippedCount", 0,
                "totalCount", 0,
                "skippedCodes", skippedCodes,
                "errors", errors
            );
        }

        for (BuyerVendor v : vendors) {
            if (v.getVendorCode() == null || v.getVendorCode().trim().isEmpty()) {
                errors.add("A row is missing Vendor Code and was skipped.");
                continue;
            }
            String rawCode = v.getVendorCode().trim();
            String normalizedCode = rawCode.toUpperCase(java.util.Locale.ROOT);

            if (v.getVendorName() == null || v.getVendorName().trim().length() < 3) {
                errors.add("Vendor '" + rawCode + "' has invalid name (min 3 chars).");
                continue;
            }
            if (v.getPhone1() == null || !v.getPhone1().matches("\\d{10}")) {
                errors.add("Vendor '" + rawCode + "' has invalid 10-digit phone: " + v.getPhone1());
                continue;
            }

            // Track normalized codes in a request-local set to avoid intra-batch duplicate constraint violations
            if (!seenCodes.add(normalizedCode)) {
                skippedCodes.add(rawCode);
                continue;
            }

            v.setVendorCode(rawCode);
            v.setBuyerOrgId(buyerOrgId);
            v.setCreatedBy(createdBy);

            if (v.getStatus() == null || v.getStatus().trim().isEmpty()) {
                v.setStatus("Active");
            }
            if (v.getSourcingScope() == null || v.getSourcingScope().trim().isEmpty()) {
                v.setSourcingScope("Client Only");
            }
            if (v.getCountry() == null || v.getCountry().trim().isEmpty()) {
                v.setCountry("IN");
            }

            try {
                if (buyerVendorDao.existsByVendorCodeAndBuyerOrgId(rawCode, buyerOrgId)) {
                    skippedCodes.add(rawCode);
                } else {
                    toSave.add(v);
                }
            } catch (Exception ex) {
                log.warn("Check exists failed for vendor code {}: {}", rawCode, ex.getMessage());
                errors.add("Vendor '" + rawCode + "' could not be checked for duplicates and was skipped.");
            }
        }

        if (!toSave.isEmpty()) {
            buyerVendorDao.saveAll(toSave);
        }

        return java.util.Map.of(
            "savedCount", toSave.size(),
            "skippedCount", skippedCodes.size(),
            "totalCount", vendors.size(),
            "skippedCodes", skippedCodes,
            "errors", errors
        );
    }
}
