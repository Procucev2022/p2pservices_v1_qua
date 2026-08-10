package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.RFQEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RFQRepository extends JpaRepository<RFQEntity, Long> {
    Optional<RFQEntity> findByRfqNumber(String rfqNumber);
    List<RFQEntity> findByBuyerEmail(String buyerEmail);
}
