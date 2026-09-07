package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.RfqAiTokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RfqAiTokenUsageRepository extends JpaRepository<RfqAiTokenUsage, Long> {
    Optional<RfqAiTokenUsage> findByRfqNumber(String rfqNumber);
}
