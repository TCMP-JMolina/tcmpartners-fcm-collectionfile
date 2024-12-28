package com.tcmp.fcupload.service.FileTypes;

import com.tcmp.fcupload.dto.CustomFields;
import com.tcmp.fcupload.dto.ProcessedLineResult;
import com.tcmp.fcupload.mdl.InvMaster;
import com.tcmp.fcupload.rep.InvMasterRepository;
import com.tcmp.fcupload.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public class UnivProcessor extends AbstractFileProcessor {
    public UnivProcessor(ProducerTemplate producerTemplate, FileUtils fileUtils, InvMasterRepository invMasterRepository) {
        super(producerTemplate, fileUtils, invMasterRepository);
    }

    @Override
    protected ProcessedLineResult processLine(String line, String clientCIF, String fileUploadId, String accountNumber) throws Exception {

        log.info("Processing UNIV Line: {}", line);

        if (line.length() < 74) {
            log.warn("Line too short, skipping: {}", line);
            throw new Exception();
        }

        String preInvoiceNumber = line.substring(0, 13).trim();
        String name = line.substring(13, 43).trim();
        String clientIdNumber = line.substring(43, 53).trim();
        String rawAmount = line.substring(53, 62).trim();
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            totalAmount = new BigDecimal(rawAmount).movePointLeft(2);
        } catch (NumberFormatException e) {
            log.error("Invalid numeric value for 'Valor': {}. Line skipped.", rawAmount);
            throw new Exception();
        }

        String dateStr = line.substring(62, 70).trim();
        LocalDate transactionDate;
        try {
            transactionDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.error("Invalid date format for 'Fecha': {}. Line skipped.", dateStr);
            throw new Exception();
        }

        String status = line.substring(70, 71).trim();
        String universityCode = line.length() > 71 ? line.substring(71, 74).trim() : "";


        String invId = UUID.randomUUID().toString().toUpperCase();

        Date date = Date.from(transactionDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        LocalDateTime expDateTime = transactionDate.plusMonths(1).atStartOfDay();
        Date expDate = Date.from(expDateTime.atZone(ZoneId.systemDefault()).toInstant());

        CustomFields customFields = new CustomFields(line);

        InvMaster invoice = createInvoice(invId,  clientCIF,  preInvoiceNumber, clientIdNumber, totalAmount,
                universityCode, name, status,  date, expDate,  fileUploadId, customFields
        );

        Map<String, String> lineData = Map.of(
                "status", "0007",
                "amount", totalAmount.toString(),
                "clientName", name,
                "accountNumber", clientIdNumber,
                "universityCode", universityCode
        );

        return new ProcessedLineResult(invoice, lineData, totalAmount);

    }


    private InvMaster createInvoice(
            String invoiceId, String clientCIF, String preInvoiceNumber, String clientIdNumber, BigDecimal totalAmount,
            String universityCode, String name, String status, Date date, Date expDate, String fileUploadId, CustomFields customFields
    ) {

        InvMaster master = new InvMaster();

        master.setInvoiceId(invoiceId);
        master.setClientCif(clientCIF);
        master.setServiceId(preInvoiceNumber);
        master.setCounterpart(clientIdNumber);
        master.setTotalAmount(totalAmount);
        master.setCurrency("USD");
        master.setPaymentMethod("Transferencia");
        master.setAccountType("UNIV");
        master.setAccountCode(universityCode);
        master.setIdType("ID");
        master.setIdCode(clientIdNumber);
        master.setFullName(name);
        master.setDescription(status + "|" + universityCode);
        master.setStatus("NEW");
        master.setSubStatus("PENDING APPROVAL");
        master.setInvoiceDate(date);
        master.setExpirationDate(expDate);
        master.setSubject("Sub");
        master.setCategory(universityCode);
        master.setUploaded(fileUploadId);
        master.setCustomFields(customFields.getFullLine());

        return master;

    }
}
