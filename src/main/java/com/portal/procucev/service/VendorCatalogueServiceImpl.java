package com.portal.procucev.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.dao.VendorCatalogueDao;
import com.portal.procucev.model.VendorCatalogue;

@Service
public class VendorCatalogueServiceImpl implements VendorCatalogueService {
	private static final Logger log = LoggerFactory.getLogger(ProcUserServiceImpl.class);

	@Autowired
	private VendorCatalogueDao vendorCatalogueDao;

	@Override
	public VendorCatalogue saveCatalogue(VendorCatalogue catalogue) {
	    log.info("Saving Vendor Catalogue: {}", catalogue);

	    if (catalogue == null) {
	        log.error("Catalogue object is null");
	        throw new IllegalArgumentException("Catalogue cannot be null");
	    }

	    VendorCatalogue savedCatalogue = vendorCatalogueDao.save(catalogue);
	    log.info("Vendor Catalogue saved successfully with ID: {}", savedCatalogue.getId());
	    return savedCatalogue;
	}

	@Override
	public List<VendorCatalogue> getCataloguesByVendorId(String id) {
	    if (id == null) {
	        log.warn("getCataloguesByVendorId() called with null id");
	        return Collections.emptyList();
	    }

	    log.info("Fetching catalogues for vendor id: {}", id);

	    List<VendorCatalogue> cataloguesList = vendorCatalogueDao.findCatalogueByVendor(id);

	    if (CollectionUtils.isEmpty(cataloguesList)) {
	        log.info("No catalogues found for vendor id: {}", id);
	        return Collections.emptyList();
	    }

	    log.info("Found {} catalogues for vendor id: {}", cataloguesList.size(), id);
	    return cataloguesList;
	}

}
