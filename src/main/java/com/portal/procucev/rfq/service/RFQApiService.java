package com.portal.procucev.rfq.service;

import com.portal.procucev.customexception.RfqDocumentSizeExceededException;
import com.portal.procucev.model.ClientDeliveryLocationRfq;
import com.portal.procucev.model.Organization;
import com.portal.procucev.model.Rfq;
import com.portal.procucev.model.RfqItem;
import com.portal.procucev.rfq.dto.RFQRequest;
import com.portal.procucev.rfq.dto.RFQResponse;
import com.portal.procucev.rfq.parser.DateParser;
import com.portal.procucev.service.AutomaticRfqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.portal.procucev.dao.MasterStatusDao;
import com.portal.procucev.model.MasterStatus;
import com.portal.procucev.utils.StatusConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class RFQApiService {

    /** Status returned when creation was refused because a document exceeded the allowed size. */
    public static final String STATUS_FILE_SIZE_EXCEEDED = "FILE_SIZE_EXCEEDED";

    private final AutomaticRfqService automaticRfqService;
    private final DateParser dateParser;
    private MasterStatusDao masterStatusDao;

    @Autowired
    public RFQApiService(AutomaticRfqService automaticRfqService, DateParser dateParser,
                         @Autowired(required = false) MasterStatusDao masterStatusDao) {
        this.automaticRfqService = automaticRfqService;
        this.dateParser = dateParser;
        this.masterStatusDao = masterStatusDao;
    }

    public RFQApiService(AutomaticRfqService automaticRfqService, DateParser dateParser) {
        this(automaticRfqService, dateParser, null);
    }

    public void setMasterStatusDao(MasterStatusDao masterStatusDao) {
        this.masterStatusDao = masterStatusDao;
    }

    public RFQResponse submitRFQ(RFQRequest request) {
        log.info("Submitting internal RFQ request for RFQ Number: {}", request.getRfqNumber());
        try {
            Rfq rfq = new Rfq();
            rfq.setRfqId(request.getRfqNumber());
            rfq.setProjectDesc(request.getProjectDesc());
            rfq.setUser(request.getUser() != null ? request.getUser() : "1");
            rfq.setSourceType(request.getSourceType() != null && !request.getSourceType().isBlank() ? request.getSourceType() : "EMAIL");
            rfq.setNoPrFlag(true);

            if (request.isIdle() || StatusConstants.CLIENT_RFQ_IDLE.equals(request.getClientStatus())) {
                if (masterStatusDao != null) {
                    MasterStatus idleStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_IDLE);
                    if (idleStatus == null) {
                        idleStatus = new MasterStatus();
                        idleStatus.setId(java.util.UUID.randomUUID().toString());
                        idleStatus.setStatus(StatusConstants.CLIENT_RFQ_IDLE);
                        idleStatus.setUiDisplay("Idle");
                        idleStatus.setDescription("Idle state for unverified demo buyer RFQs");
                        try {
                            idleStatus = masterStatusDao.saveAndFlush(idleStatus);
                        } catch (Exception e) {
                            log.warn("Could not save new CLIENT_RFQ_IDLE MasterStatus: {}", e.getMessage());
                            idleStatus = masterStatusDao.findByStatus(StatusConstants.CLIENT_RFQ_IDLE);
                        }
                    }
                    if (idleStatus != null) {
                        rfq.setClientStatus(idleStatus);
                    }
                }
            }

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
                    del.setRfq(rfq);
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
                    // Quantity and unit of measure are deliberately passed through as extracted.
                    // raiseRfq applies the shared "default a missing quantity to 1, a missing UOM to
                    // Nos" rule for every source, so defaulting here as well would only let the two
                    // rules drift apart.
                    item.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0.0);
                    item.setUnitofMeasures(dto.getUnitofMeasures());
                    item.setBrand(dto.getBrand());
                    item.setItemcode(dto.getItemcode());
                    item.setRemarks(dto.getRemarks());
                    item.setSerialNo(dto.getSerialNo());
                    itemsList.add(item);
                }
                rfq.setRfqItem(itemsList);
            }

            // Documents are built by the shared pipeline, which owns both the per-file size limit and
            // the column mapping. The email path used to map them itself and wrote the payload into
            // file_details as well as file; file_details is a 64KB BLOB, so every attachment over
            // 64KB failed the insert and rolled the whole RFQ back.
            automaticRfqService.attachDocuments(rfq, request.getRfqDocument());

            boolean success = automaticRfqService.raiseRfq(rfq);

            return RFQResponse.builder()
                    .rfqNumber(request.getRfqNumber())
                    .status(success ? "SUCCESS" : "FAILED")
                    .buyerEmail(request.getBuyerEmail())
                    .message(success ? "RFQ created successfully internally." : "Failed to create RFQ internally.")
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (RfqDocumentSizeExceededException e) {
            // Reported with its own status so the caller can acknowledge an oversized attachment as
            // such instead of as a generic creation failure.
            log.warn("RFQ {} rejected because an attachment exceeds the allowed size: {}",
                    request.getRfqNumber(), e.getMessage());
            return RFQResponse.builder()
                    .rfqNumber(request.getRfqNumber())
                    .status(STATUS_FILE_SIZE_EXCEEDED)
                    .buyerEmail(request.getBuyerEmail())
                    .message(e.getMessage())
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
