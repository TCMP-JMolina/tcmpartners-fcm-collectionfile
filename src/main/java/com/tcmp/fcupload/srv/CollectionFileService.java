package com.tcmp.fcupload.srv;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.tcmp.fcupload.dto.CustomFields;
import com.tcmp.fcupload.dto.request.FndtMsg;
import com.tcmp.fcupload.dto.response.FndtMsgResponse;
import com.tcmp.fcupload.dto.response.Header;
import com.tcmp.fcupload.dto.response.Msg;
import com.tcmp.fcupload.dto.response.ResponseDetails;
import com.tcmp.fcupload.mdl.InvBill;
import com.tcmp.fcupload.mdl.InvoiceMaster;
import com.tcmp.fcupload.rep.InvBillRep;
import com.tcmp.fcupload.rep.InvoiceMasterRepository;
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
public class CollectionFileService {

    private final InvoiceMasterRepository invoiceMasterRepository;
    private final InvBillRep invoiceBillRepository;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Async
    @Transactional
    public void processCollectionFileRequest(String body) throws JsonProcessingException {
        FndtMsg request = parseRequest(body);

        List<InvoiceMaster> invMasterList = getInvoiceMasters(request);

        List<InvBill> invBillList = processInvoiceMasters(invMasterList);
        saveEntities(invBillList);
        sendResponse(request);
    }

    private FndtMsg parseRequest(String body) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        return xmlMapper.readValue(body, FndtMsg.class);
    }

    private List<InvoiceMaster> getInvoiceMasters(FndtMsg request) {
       log.info("IDde carga: "+request.getMsg().getExtn().getPirReferenceNumber());
        return invoiceMasterRepository.findByUploadedAndSubStatus(
                request.getMsg().getExtn().getPirReferenceNumber(), "SENT TO BANK"
        );
    }

    private List<InvBill> processInvoiceMasters(List<InvoiceMaster> invMasterList) {
        List<InvBill> invBillList = new ArrayList<>();
        for (InvoiceMaster invMaster : invMasterList) {
            invMaster.setSubStatus("PROCESSED");
            invBillList.add(createAndSaveInvBill(invMaster));
        }
        try {
            invoiceMasterRepository.saveAll(invMasterList);
        } catch (Exception e) {
            log.error("Error while saving InvoiceMaster : {}", e.getMessage(), e);
        }

        return invBillList;
    }

    private void saveEntities(List<InvBill> invBillList) {
        try {
            invoiceBillRepository.saveAll(invBillList);
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

    private InvBill createAndSaveInvBill(InvoiceMaster invMaster) {

        InvBill invBill = new InvBill();

        invBill.setBillId(invMaster.getInvoiceId());
        invBill.setCliCIF(invMaster.getClientCif());
        invBill.setServiceId(invMaster.getServiceId());
        invBill.setBillCpart(invMaster.getCounterpart());
        invBill.setBillTotal(invMaster.getTotalAmount());
        invBill.setBillCcy(invMaster.getCurrency());
        invBill.setBillPMethod(invMaster.getPaymentMethod());
        invBill.setBillAccType(invMaster.getAccountType());
        invBill.setBillAccCode(invMaster.getAccountCode());
        invBill.setBillIdType(invMaster.getIdType());
        invBill.setBillIdCode(invMaster.getIdCode());
        invBill.setBillFullName(invMaster.getFullName());
        invBill.setBillDesc(invMaster.getDescription());
        invBill.setBillStatus(invMaster.getStatus());
        invBill.setBillSubStatus("PROCESSED");
        invBill.setBillDate(invMaster.getInvoiceDate());
        invBill.setBillExpiration(invMaster.getExpirationDate());
        invBill.setBillSubject(invMaster.getSubject());
        invBill.setBillCategory(invMaster.getCategory());
        invBill.setBillUpload(invMaster.getUploaded());
        invBill.setBillCustomFields(invMaster.getCustomFields());

        return invBill;
    }


}
