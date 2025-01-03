//package com.tcmp.fcupload.service;
//
//import com.azure.storage.blob.BlobClient;
//import com.azure.storage.blob.BlobServiceClient;
//import com.azure.storage.blob.models.BlobProperties;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.tcmp.fcupload.config.BlobClientConfig;
//import com.tcmp.fcupload.dto.CustomFields;
//import com.tcmp.fcupload.dto.ProcessedLineResult;
//import com.tcmp.fcupload.dto.recordType.*;
//import com.tcmp.fcupload.model.InvMaster;
//import com.tcmp.fcupload.repository.InvMasterRepository;
//import com.tcmp.fcupload.utils.FileUtils;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.camel.Exchange;
//import org.apache.camel.ProducerTemplate;
//import org.apache.camel.component.azure.storage.blob.BlobConstants;
//import org.springframework.cache.CacheManager;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.*;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ProcessFileService {
//
//    private final ProducerTemplate producerTemplate;
//    private final FileUtils fileUtils;
//    private final InvMasterRepository invMasterRepository;
//    private final CacheManager cacheManager;
//
//    private List<InvMaster> invMasterBatch = new ArrayList<>();
//    private List<Map<String, String>> processedLines = new ArrayList<>();
//    private List<Map<String, String>> basicIntrumentList;
//    private Integer rejectedLines = 0;
//    private Integer successLines = 0;
//    private BigDecimal totalAmount = BigDecimal.ZERO;
//
//
//    public void processLineByLine(List<String> lines, String fileUploadId, String cliCIF, String orderId,
//                                  String uploadUser, String subserviceId, String subservice, String accountNumber) throws Exception {
//        log.info("Processing lines. Total lines: {}", lines.size());
//
//
//        for (String line : lines) {
//            processLine(line);
//        }
//        invMasterRepository.saveAll(invMasterBatch);
//
//        sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
//                subservice, accountNumber, successLines.toString(), rejectedLines.toString());
//
//        basicIntrumentList = processedLines;
//        cacheManager.getCache("batchCache").put("successLines", successLines);
//        cacheManager.getCache("batchCache").put("rejectedLines", rejectedLines);
//        log.info("Finished processing. Total inserted: Rows={}", invMasterBatch.size());
//        log.info("Processing completed. Success: {}, Rejected: {}", successLines, rejectedLines);
//
//    }
//
//
//    private void processLine(String line) throws Exception {
//
//        log.info("Processing the line: {}", line);
//
//        String[] parts = line.split(",", -1);
//        if (parts.length < 2) {
//            throw new IllegalArgumentException("Invalid line format. Not enough fields.");
//        }
//
//        String recordType = parts[0].trim();
//
//        switch (recordType) {
//            case "H":
//                processHeader(parts);
//
//            case "D":
//                try {
//                    createInvoice(processDetails(parts), line);
//                    successLines++;
//                } catch (Exception e) {
//                    log.error("Failed to process line: {}", line, e);
//                    processedLines.add(handleProcessingError());
//                    rejectedLines++;
//
//                }
//
//            case "DE":
//                processBreakdown(parts);
//
//            case "E":
//                processEnrichments(parts);
//
//            case "T":
//                processFooter(parts);
//
//            default:
//                throw new IllegalArgumentException("Unknown record type: " + recordType);
//        }
//
//
//    }
//
//    private Header processHeader(String[] parts) {
//        Header header = new Header();
//        header.setClientName(parts[1].trim());
//        header.setPackageName(parts[2].trim());
//        header.setPaymentCurrency(parts[3].trim());
//        header.setDebitAccount(parts[4].trim());
//        header.setTransactionDate(parts[5].trim());
//        header.setPirReferenceNumber(parts[6].trim());
//
//        log.info("Header processed successfully");
//        return header;
//    }
//
//    private Details processDetails(String[] parts) {
//        Details details = new Details();
//        details.setInstrumentUniqueReference(parts[1].trim());
//        details.setInstrumentReference(parts[2].trim());
//        details.setPaymentProduct(parts[3].trim());
//        details.setPackageName(parts[4].trim());
//        details.setInstrumentNumber(parts[5].trim());
//        details.setInstrumentAmount(parts[6].trim());
//        details.setInstrumentCurrency(parts[7].trim());
//        details.setDebitAccount(parts[8].trim());
//        details.setDebitReferenceNumber(parts[9].trim());
//        details.setDebitDescription(parts[10].trim());
//        details.setCreditReferenceNumber(parts[11].trim());
//        details.setCreditDescription(parts[12].trim());
//        details.setBenefName(parts[13].trim());
//        details.setBeneficiaryBankBicCode(parts[14].trim());
//        details.setBeneficiaryBankName(parts[15].trim());
//        details.setBeneficiaryBankAddressLine1(parts[16].trim());
//        details.setBeneficiaryBankAddressLine2(parts[17].trim());
//        details.setBeneficiaryBankAddressLine3(parts[18].trim());
//        details.setBeneficiaryBankPostalCode(parts[19].trim());
//        details.setBeneficiaryBankCity(parts[20].trim());
//        details.setBeneficiaryBankState(parts[21].trim());
//        details.setBeneficiaryBankCountry(parts[22].trim());
//        details.setFxContractRate(parts[23].trim());
//        details.setFxRateType(parts[24].trim());
//        details.setBeneficiaryAccount(parts[25].trim());
//        details.setBeneficiaryAddressLine1(parts[26].trim());
//        details.setBeneficiaryAddressLine2(parts[27].trim());
//        details.setBeneficiaryAddressLine3(parts[28].trim());
//        details.setBeneficiaryPostalCode(parts[29].trim());
//        details.setBeneficiaryCountry(parts[30].trim());
//        details.setBeneficiaryEmailId(parts[31].trim());
//        details.setBeneficiaryMobileNumber(parts[32].trim());
//        details.setCorrBankAddress1(parts[33].trim());
//        details.setIntermediaryBankIdType(parts[34].trim());
//        details.setIntermediaryBankCode(parts[35].trim());
//        details.setIntermediaryBankName(parts[36].trim());
//        details.setIntermediaryBankBic(parts[37].trim());
//        details.setIntermediaryBankNostroAccount(parts[38].trim());
//        details.setIntermediaryBankAddress1(parts[39].trim());
//        details.setIntermediaryBankCity(parts[40].trim());
//        details.setIntermediaryBankCountry(parts[41].trim());
//        details.setIntermediaryBankDetails1(parts[42].trim());
//        details.setBeneficiaryBankId(parts[43].trim());
//        details.setBeneficiaryBankIdType(parts[44].trim());
//        details.setRemittanceInfo1(parts[45].trim());
//        details.setRemittanceInfo2(parts[46].trim());
//        details.setRemittanceInfo3(parts[47].trim());
//        details.setRemittanceInfo4(parts[48].trim());
//        details.setStandingInstructionStartDate(parts[49].trim());
//        details.setStandingInstructionEndDate(parts[50].trim());
//        details.setStandingInstructionTypeOfDate(parts[51].trim());
//        details.setStandingInstructionFrequency(parts[52].trim());
//        details.setStandingInstructionPeriod(parts[53].trim());
//        details.setStandingInstructionReferenceDay(parts[54].trim());
//        details.setStandingInstructionHolidayAction(parts[55].trim());
//        details.setEnrichmentValue1(parts[56].trim());
//        details.setEnrichmentValue2(parts[57].trim());
//        details.setEnrichmentValue3(parts[58].trim());
//        details.setEnrichmentValue4(parts[59].trim());
//        details.setEnrichmentValue5(parts[60].trim());
//        details.setEnrichmentValue6(parts[61].trim());
//        details.setEnrichmentValue7(parts[62].trim());
//        details.setEnrichmentValue8(parts[63].trim());
//        details.setEnrichmentValue9(parts[64].trim());
//        details.setEnrichmentValue10(parts[65].trim());
//        details.setEnrichmentValue11(parts[66].trim());
//        details.setEnrichmentValue12(parts[67].trim());
//        details.setEnrichmentValue13(parts[68].trim());
//        details.setEnrichmentValue14(parts[69].trim());
//        details.setEnrichmentValue15(parts[70].trim());
//        details.setEnrichmentValue16(parts[71].trim());
//        details.setEnrichmentValue17(parts[72].trim());
//        details.setEnrichmentValue18(parts[73].trim());
//        details.setEnrichmentValue19(parts[74].trim());
//        details.setEnrichmentValue20(parts[75].trim());
//        details.setRegulatoryReporting1(parts[76].trim());
//        details.setRegulatoryReporting2(parts[77].trim());
//        details.setRegulatoryReporting3(parts[78].trim());
//        details.setRegulatoryReporting4(parts[79].trim());
//        details.setRegulatoryReporting5(parts[80].trim());
//
//        log.info("Details processed successfully");
//
//        return details;
//    }
//
//    private Breakdown processBreakdown(String[] parts) {
//        Breakdown breakdown = new Breakdown();
//        breakdown.setEnrichment11(parts[1].trim());
//        breakdown.setEnrichment12(parts[2].trim());
//        breakdown.setEnrichment13(parts[3].trim());
//        breakdown.setEnrichment14(parts[4].trim());
//        breakdown.setEnrichment15(parts[5].trim());
//        breakdown.setEnrichment16(parts[6].trim());
//
//        log.info("Breakdown processed: {}", breakdown);
//        return breakdown;
//    }
//
//    private Enrichments processEnrichments(String[] parts) {
//        Enrichments enrichments = new Enrichments();
//        enrichments.setEnrichment1(parts[0].trim());
//        enrichments.setEnrichment2(parts[1].trim());
//        enrichments.setEnrichment3(parts[2].trim());
//        enrichments.setEnrichment4(parts[3].trim());
//        enrichments.setEnrichment5(parts[4].trim());
//        enrichments.setEnrichment6(parts[5].trim());
//        enrichments.setEnrichment7(parts[6].trim());
//        enrichments.setEnrichment8(parts[7].trim());
//        enrichments.setEnrichment9(parts[8].trim());
//
//        log.info("Enrichments processed successfully");
//        return enrichments;
//    }
//
//    private Footer processFooter(String[] parts) {
//        Footer footer = new Footer();
//        footer.setTotalInstrument(parts[1].trim());
//        footer.setTotalAmount(parts[2].trim());
//
//        log.info("Footer processed sucessfully");
//        return footer;
//    }
//
//
//    private InvMaster createInvoice(Details details, String line) {
//
//
//        String invoiceId = UUID.randomUUID().toString().toUpperCase();
//
//        LocalDateTime now = LocalDateTime.now();
//        Date startDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
//        LocalDateTime expDate = now.plusMonths(1);
//        Date expirationDate = Date.from(expDate.atZone(ZoneId.systemDefault()).toInstant());
//
//        InvMaster master = new InvMaster();
//
//        master.setInvoiceId(invoiceId);
//        master.setClientCif(clientCIF);//***
//        master.setServiceId(counterpartCode);//***
//        master.setCounterpart(details.getInstrumentReference());
//        master.setTotalAmount(new BigDecimal(details.getInstrumentAmount()));
//        master.setCurrency(details.getInstrumentCurrency());
//        master.setPaymentMethod(details.getPaymentProduct());
//        master.setAccountType(details.getDebitReferenceNumber());//***
//        master.setAccountCode(details.getDebitAccount());//***
//        master.setIdType();//***
//        master.setIdCode();//***
//        master.setFullName();//***
//        master.setDescription(details.getDebitDescription());//***
//        master.setStatus("NEW");
//        master.setSubStatus("PENDING APPROVAL");
//        master.setInvoiceDate(startDate);
//        master.setExpirationDate(expirationDate);
//        master.setSubject("Sub");
//        master.setCategory(orientationCode);//***
//        master.setUploaded(fileUploadId);//***
//        master.setCustomFields(line);
//
//        Map<String, String> lineData = Map.of(
//                "status", "0007",
//                "amount", details.getInstrumentAmount(),
//                "clientName", "",//***
//                "accountNumber", ""//***
//        );
//
//        invMasterBatch.add(master);
//        processedLines.add(lineData);
//        totalAmount = totalAmount.add(new BigDecimal(details.getInstrumentAmount()));
//
//        return master;
//    }
//
//    private void sendPayment(int size, BigDecimal totalAmount, String fileUploadId,
//                             String CIF, String orderId, String uploadUser, String subserviceId,
//                             String subservice, String accoutNumber, String linesOK, String linesError) {
//        try {
//            // Create message JSON to direct:SendPayment
//            Map<String, Object> paymentData = new HashMap<>();
//            paymentData.put("size", size);
//            paymentData.put("totalAmount", totalAmount);
//            paymentData.put("fileUploadId", fileUploadId);
//            paymentData.put("orderID", orderId);
//            paymentData.put("uploadUser", uploadUser);
//            paymentData.put("subserviceId", subserviceId);
//            paymentData.put("subservice", subservice);
//            paymentData.put("cif", CIF);
//            paymentData.put("accountNumber", accoutNumber);
//            paymentData.put("linesOk", linesOK);
//            paymentData.put("linesError", linesError);
//
//
//            ObjectMapper objectMapper = new ObjectMapper();
//            String jsonPayload = objectMapper.writeValueAsString(paymentData);
//
//            log.info("Sending JSON to Kafka: {}", jsonPayload);
//            producerTemplate.sendBody("direct:SendPayment", jsonPayload);
//
//        } catch (Exception e) {
//            log.error("Error sending JSON to Kafka", e);
//        }
//    }
//
//    private Map<String, String> handleProcessingError() {
//        return Map.of(
//                "status", "0008",
//                "amount", "0",
//                "clientName", "",
//                "accountNumber", ""
//        );
//    }
//
//}
