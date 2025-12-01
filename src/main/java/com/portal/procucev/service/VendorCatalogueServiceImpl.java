package com.portal.procucev.service;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.portal.procucev.dao.BFSDao;
import com.portal.procucev.dao.VendorCatalogueDao;
import com.portal.procucev.dao.VendorTermsConditionsDao;
import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.VendorCatalogue;
import com.portal.procucev.model.VendorTermsConditions;

@Service
public class VendorCatalogueServiceImpl implements VendorCatalogueService {
	private static final Logger log = LoggerFactory.getLogger(ProcUserServiceImpl.class);

	@Autowired
	private VendorCatalogueDao vendorCatalogueDao;
	
	@Autowired
	private VendorTermsConditionsDao termsDao;
	
	@Autowired
	private BFSDao bfsDao;
	
	@Override
	public VendorCatalogue saveCatalogue(VendorCatalogue catalogue) {
	    log.info("Saving Vendor Catalogue: {}", catalogue);

	    if (catalogue == null) {
	        log.error("Catalogue object is null");
	        throw new IllegalArgumentException("Catalogue cannot be null");
	    }

	    VendorCatalogue savedCatalogue = vendorCatalogueDao.save(catalogue);
	    BFSItems bfsItem = new BFSItems();
	    bfsItem.setDescription(catalogue.getMaterialDescription());
	    bfsItem.setUnitofMeasures(catalogue.getUom());
	    bfsItem.setTotalQuantity(catalogue.getMinOrderQuantity());
	    bfsItem.setAvailableQuantity(catalogue.getAvailableQuantity());
	    bfsItem.setSellPrice(catalogue.getPricePerUom().doubleValue());
	    bfsItem.setAskPrice(catalogue.getPricePerUom().doubleValue());
	    bfsItem.setUserId(catalogue.getUser());
	    // set org based on dto.getOrg().getId()
	     bfsItem.setOrg(catalogue.getOrg());

	    bfsDao.save(bfsItem);
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

	@Override
	public VendorTermsConditions saveTermsAndConditions(VendorTermsConditions conditions) {
		// TODO Auto-generated method stub
		 log.info("Saving Vendor Catalogue: {}", conditions);

		    if (conditions == null) {
		        log.error("Terms Conditions object is null");
		        throw new IllegalArgumentException("Terms Conditions cannot be null");
		    }

		    VendorTermsConditions savedTC = termsDao.save(conditions);
		    log.info("Vendor Terms Comditions saved successfully with ID: {}", savedTC.getId());
		    return savedTC;
	}

	@Override
	public List<VendorTermsConditions> getTCByVendorId(String id) {
		// TODO Auto-generated method stub
		  if (id == null) {
		        log.warn("get Terms and Conditions By VendorId() called with null id");
		        return Collections.emptyList();
		    }

		    log.info("Fetching Terms and Conditions for vendor id: {}", id);

		    List<VendorTermsConditions> tcList = termsDao.findByOrgIdOrderByCreatedTSDesc(id);

		    if (CollectionUtils.isEmpty(tcList)) {
		        log.info("No Terms and Conditions found for vendor id: {}", id);
		        return Collections.emptyList();
		    }

		    log.info("Found {} Terms and Conditions for vendor id: {}", tcList.size(), id);
		    return tcList;
	}

}
