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
}
