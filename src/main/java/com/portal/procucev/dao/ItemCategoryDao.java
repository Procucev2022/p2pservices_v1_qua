package com.portal.procucev.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.portal.procucev.model.ItemCategory;

public interface ItemCategoryDao extends JpaRepository<ItemCategory, String> {

}
