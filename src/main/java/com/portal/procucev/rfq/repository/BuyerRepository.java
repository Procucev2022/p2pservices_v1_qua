package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.BuyerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuyerRepository extends JpaRepository<BuyerEntity, Long> {
    Optional<BuyerEntity> findByEmail(String email);
    Optional<BuyerEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
}
