package com.portal.procucev.service;

import java.util.List;

import com.portal.procucev.model.VendorCatalogue;
import com.portal.procucev.model.VendorTermsConditions;

public interface VendorCatalogueService {

	VendorCatalogue saveCatalogue(VendorCatalogue catalogue);

	List<VendorCatalogue> getCataloguesByVendorId(String id);

	VendorTermsConditions saveTermsAndConditions(VendorTermsConditions conditions);

	List<VendorTermsConditions> getTCByVendorId(String id);

}
