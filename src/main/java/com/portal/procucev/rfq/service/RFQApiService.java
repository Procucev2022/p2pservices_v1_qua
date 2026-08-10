package com.portal.procucev.rfq.service;

import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.service.AutomaticRfqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RFQApiService {

    private final AutomaticRfqService automaticRfqService;
    private final DateParser dateParser;

    public RFQResponse submitRFQ(RFQRequest request) {
        log.info("Submitting internal RFQ request for RFQ Number: {}", request.getRfqNumber());
        try {
            Rfq rfq = new Rfq();
            rfq.setRfqId(request.getRfqNumber());
            rfq.setProjectDesc(request.getProjectDesc());
            rfq.setUser(request.getUser() != null ? request.getUser() : "1");
            rfq.setSourceType("T");
            rfq.setNoPrFlag(true);

            if (request.getDeliveryDate() != null && !request.getDeliveryDate().isBlank()) {
                Date delivDate = dateParser.parseToDate(request.getDeliveryDate());
                rfq.setDeliveryDate(delivDate);
            }

            if (request.getClientdeliverylocationrfq() != null && !request.getClientdeliverylocationrfq().isEmpty()) {
                List<ClientDeliveryLocationRfq> locList = new ArrayList<>();
                for (RFQRequest.LocationDto loc : request.getClientdeliverylocationrfq()) {
                    ClientDeliveryLocationRfq del = new ClientDeliveryLocationRfq();
                    del.setCity(loc.getCity());
                    del.setState(loc.getState());
                    del.setPincode(loc.getPincode());
                    locList.add(del);
                }
                rfq.setClientdeliverylocationrfq(locList);
            }

            if (request.getRfqItem() != null && !request.getRfqItem().isEmpty()) {
                List<RfqItem> itemsList = new ArrayList<>();
                for (RFQRequest.RfqItemDto dto : request.getRfqItem()) {
                    RfqItem item = new RfqItem();
                    item.setDescription(dto.getDescription());
                    item.setCategory(dto.getCategory());
                    item.setQuantity((double) dto.getQuantity());
                    item.setUnitofMeasures(dto.getUnitofMeasures());
                    item.setBrand(dto.getBrand());
                    item.setRemarks(dto.getRemarks());
                    item.setSerialNo(dto.getSerialNo());
                    itemsList.add(item);
                }
                rfq.setRfqItem(itemsList);
            }

            boolean success = automaticRfqService.raiseRfq(rfq);

            return RFQResponse.builder()
                    .rfqNumber(request.getRfqNumber())
                    .status(success ? "SUCCESS" : "FAILED")
                    .buyerEmail(request.getBuyerEmail())
                    .message(success ? "RFQ created successfully internally." : "Failed to create RFQ internally.")
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error creating RFQ internally for RFQ Number {}: {}", request.getRfqNumber(), e.getMessage(), e);
            return RFQResponse.builder()
                    .rfqNumber(request.getRfqNumber())
                    .status("FAILED")
                    .buyerEmail(request.getBuyerEmail())
                    .message("Internal RFQ creation error: " + e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .build();
        }
    }
}
