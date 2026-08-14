package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.CategoryItemMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryItemMappingRepository extends JpaRepository<CategoryItemMappingEntity, Long> {
    List<CategoryItemMappingEntity> findByNormalizedDescription(String normalizedDescription);
}
