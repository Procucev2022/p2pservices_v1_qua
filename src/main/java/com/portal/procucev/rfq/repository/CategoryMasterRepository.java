package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.CategoryMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryMasterRepository extends JpaRepository<CategoryMasterEntity, Long> {
    List<CategoryMasterEntity> findByActiveTrue();
}
