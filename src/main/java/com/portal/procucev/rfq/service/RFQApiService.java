package com.portal.procucev.rfq.service;

import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.RFQDocument;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.service.AutomaticRfqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
            if (request.getOrg() != null && request.getOrg().getId() != null && !request.getOrg().getId().isBlank()) {
                Organization org = new Organization();
                org.setId(request.getOrg().getId());
                rfq.setOrg(org);
            }

            if (request.getDeliveryDate() != null && !request.getDeliveryDate().isBlank()) {
                Date delivDate = dateParser.parseToDate(request.getDeliveryDate());
                rfq.setDeliveryDate(delivDate);
            }

            if (request.getClientdeliverylocationrfq() != null && !request.getClientdeliverylocationrfq().isEmpty()) {
                List<ClientDeliveryLocationRfq> locList = new ArrayList<>();
                for (RFQRequest.LocationDto loc : request.getClientdeliverylocationrfq()) {
                    ClientDeliveryLocationRfq del = new ClientDeliveryLocationRfq();
                    del.setAddress(loc.getAddress());
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
                    if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
                        throw new IllegalArgumentException("Quantity is mandatory for RFQ item: " + dto.getDescription());
                    }
                    item.setQuantity(dto.getQuantity());
                    item.setUnitofMeasures(dto.getUnitofMeasures());
                    item.setBrand(dto.getBrand());
                    item.setItemcode(dto.getItemcode());
                    item.setRemarks(dto.getRemarks());
                    item.setSerialNo(dto.getSerialNo());
                    itemsList.add(item);
                }
                rfq.setRfqItem(itemsList);
            }

            if (request.getRfqDocument() != null && !request.getRfqDocument().isEmpty()) {
                List<RFQDocument> documents = new ArrayList<>();
                for (Map<String, String> documentMap : request.getRfqDocument()) {
                    String fileName = documentMap.get("fileName");
                    String encodedFile = documentMap.get("file");
                    if (encodedFile == null || encodedFile.isBlank()) {
                        continue;
                    }
                    RFQDocument rfqDocument = new RFQDocument();
                    rfqDocument.setFileName(fileName);
                    byte[] bytes = Base64.getDecoder().decode(encodedFile);
                    rfqDocument.setFile(bytes);
                    rfqDocument.setFileDetails(bytes);
                    rfqDocument.setVersion(1);
                    documents.add(rfqDocument);
                }
                rfq.setRfqDocument(documents);
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
