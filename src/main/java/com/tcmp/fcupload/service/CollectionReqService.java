package com.tcmp.fcupload.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tcmp.fcupload.dto.request.FndtMsg;
import com.tcmp.fcupload.dto.response.FndtMsgResponse;
import com.tcmp.fcupload.dto.response.Header;
import com.tcmp.fcupload.dto.response.Msg;
import com.tcmp.fcupload.dto.response.ResponseDetails;
import com.tcmp.fcupload.mdl.InvBiller;
import com.tcmp.fcupload.mdl.InvMaster;
import com.tcmp.fcupload.rep.InvBillerRepository;
import com.tcmp.fcupload.rep.InvMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionReqService {

    private final InvMasterRepository invoiceMasterRepository;
    private final InvBillerRepository invoiceBillRepository;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Async
    @Transactional
    public void processCollectionFileRequest(String body) throws JsonProcessingException {
        FndtMsg request = parseRequest(body);

        List<InvMaster> invMasterList = getInvoiceMasters(request);
        log.info("Founded {} registers in the InvoiceMaster with coincidences", invMasterList.size());
        List<InvBiller> invBillerList = processInvoiceMasters(invMasterList);
        saveEntities(invBillerList);
        sendResponse(request);
    }

    private FndtMsg parseRequest(String body) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(body, FndtMsg.class);
    }

    private List<InvMaster> getInvoiceMasters(FndtMsg request) {
       log.info("Upload Id: "+request.getMsg().getExtn().getPirReferenceNumber());
        return invoiceMasterRepository.findByUploadedAndSubStatus(
                request.getMsg().getExtn().getPirReferenceNumber(), "PENDING APPROVAL"
        );
    }

    private List<InvBiller> processInvoiceMasters(List<InvMaster> invMasterList) {
        List<InvBiller> invBillerList = new ArrayList<>();
        for (InvMaster invMaster : invMasterList) {
            invMaster.setSubStatus("APPROVED");
            invBillerList.add(createAndSaveInvBill(invMaster));
        }
        try {
            invoiceMasterRepository.saveAll(invMasterList);
            log.info("Changed the status of {} registers in the InvoiceMaster with APPROVED", invMasterList.size());
        } catch (Exception e) {
            log.error("Error while saving InvoiceMaster : {}", e.getMessage(), e);
        }

        return invBillerList;
    }

    private void saveEntities(List<InvBiller> invBillerList) {
        try {
            invoiceBillRepository.saveAll(invBillerList);
            log.info("Saving {} new registers in the InvoiceBiller", invBillerList.size());
        } catch (Exception e) {
            log.error("Error while saving InvoiceBiller: {}", e.getMessage(), e);
        }
    }

    private void sendResponse(FndtMsg request) throws JsonProcessingException {
        FndtMsgResponse response = createResponse(request);
        String message = convertToXml(response);
        producerTemplate.sendBody("direct:sendTo_COLLECTIONFILE_RESP", message);
    }

    private String convertToXml(FndtMsgResponse response) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.writeValueAsString(response);
    }

    private FndtMsgResponse createResponse(FndtMsg request) {
        Header header = new Header(
                request.getHeader().getSesExecuId(), "PAY_TXN_HANDOFF", request.getMsg().getPmnt().getDocument().getPirIntNumb()
        );
        Msg msg = new Msg("00001", "00001", request.getMsg().getPmnt().getDocument().getPirTotAmnt());
        ResponseDetails responseDetails = new ResponseDetails("GC", "0001", "PROCESO EXITOSO", "", "LP", "", "PROCESO EXITOSO", "");

        return new FndtMsgResponse(header, msg, responseDetails);
    }

    private InvBiller createAndSaveInvBill(InvMaster invMaster) {

        InvBiller biller = new InvBiller();

        biller.setId(invMaster.getInvoiceId());
        biller.setClientCIF(invMaster.getClientCif());
        biller.setServiceId(invMaster.getServiceId());
        biller.setCounterpart(invMaster.getCounterpart());
        biller.setTotalAmount(invMaster.getTotalAmount());
        biller.setCurrency(invMaster.getCurrency());
        biller.setPaymentMethod(invMaster.getPaymentMethod());
        biller.setAccountType(invMaster.getAccountType());
        biller.setAccountCode(invMaster.getAccountCode());
        biller.setIdType(invMaster.getIdType());
        biller.setIdCode(invMaster.getIdCode());
        biller.setFullName(invMaster.getFullName());
        biller.setDescription(invMaster.getDescription());
        biller.setStatus(invMaster.getStatus());
        biller.setSubStatus("APPROVED");
        biller.setDate(invMaster.getInvoiceDate());
        biller.setExpirationDate(invMaster.getExpirationDate());
        biller.setSubject(invMaster.getSubject());
        biller.setCategory(invMaster.getCategory());
        biller.setUploadedFileId(invMaster.getUploaded());
        biller.setCustomFields(invMaster.getCustomFields());

        return biller;
    }


}
