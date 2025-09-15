package com.portal.procucev.service;

import java.util.List;

import com.portal.procucev.model.VendorCatalogue;

public interface VendorCatalogueService {

	VendorCatalogue saveCatalogue(VendorCatalogue catalogue);

	List<VendorCatalogue> getCataloguesByVendorId(String id);

}
