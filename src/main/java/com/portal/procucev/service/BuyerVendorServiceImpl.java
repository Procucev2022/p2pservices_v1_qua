package com.portal.procucev.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.portal.procucev.dao.BuyerVendorDao;
import com.portal.procucev.model.BuyerVendor;

import java.util.Optional;

@Service
public class BuyerVendorServiceImpl implements BuyerVendorService {

    @Autowired
    private BuyerVendorDao buyerVendorDao;

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
    public Optional<BuyerVendor> getVendorById(String id, String buyerOrgId) {
        return buyerVendorDao.findByIdAndBuyerOrgId(id, buyerOrgId);
    }

    @Override
    public BuyerVendor updateVendor(String id, BuyerVendor vendor, String buyerOrgId) {
        Optional<BuyerVendor> existing = buyerVendorDao.findByIdAndBuyerOrgId(id, buyerOrgId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Vendor not found");
        }
        BuyerVendor entity = existing.get();
        entity.setVendorCode(vendor.getVendorCode());
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
    public boolean updateStatus(String id, String buyerOrgId, String status) {
        return buyerVendorDao.updateStatus(id, buyerOrgId, status) > 0;
    }

    @Override
    public java.util.Map<String, Object> bulkCreateVendors(java.util.List<BuyerVendor> vendors, String buyerOrgId, String createdBy) {
        java.util.List<BuyerVendor> toSave = new java.util.ArrayList<>();
        java.util.List<String> skippedCodes = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

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
            if (v.getVendorName() == null || v.getVendorName().trim().length() < 3) {
                errors.add("Vendor '" + v.getVendorCode() + "' has invalid name (min 3 chars).");
                continue;
            }
            if (v.getPhone1() == null || !v.getPhone1().matches("\\d{10}")) {
                errors.add("Vendor '" + v.getVendorCode() + "' has invalid 10-digit phone: " + v.getPhone1());
                continue;
            }

            v.setVendorCode(v.getVendorCode().trim());
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

            if (buyerVendorDao.existsByVendorCodeAndBuyerOrgId(v.getVendorCode(), buyerOrgId)) {
                skippedCodes.add(v.getVendorCode());
            } else {
                toSave.add(v);
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
