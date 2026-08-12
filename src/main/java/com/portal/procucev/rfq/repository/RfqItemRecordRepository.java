package com.portal.procucev.rfq.repository;

import com.portal.procucev.rfq.entity.RfqItemRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RfqItemRecordRepository extends JpaRepository<RfqItemRecord, Long> {
    List<RfqItemRecord> findByBuyerEmailAndItemDescriptionIgnoreCaseAndDeliveryDate(
            String buyerEmail, String itemDescription, String deliveryDate);
}
