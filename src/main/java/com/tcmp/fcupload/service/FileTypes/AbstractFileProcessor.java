package com.tcmp.fcupload.service.FileTypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcmp.fcupload.dto.ProcessedLineResult;
import com.tcmp.fcupload.mdl.InvMaster;
import com.tcmp.fcupload.rep.InvMasterRepository;
import com.tcmp.fcupload.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.*;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractFileProcessor implements FileProcessor {

    protected final ProducerTemplate producerTemplate;
    protected final FileUtils fileUtils;
    protected final InvMasterRepository invMasterRepository;

    protected List<InvMaster> invMasterBatch = new ArrayList<>();
    protected List<Map<String, String>> processedLines = new ArrayList<>();
    protected List<Map<String, String>> basicIntrumentList;
    protected Integer rejectedLines = 0;
    protected Integer successLines = 0;


    protected abstract ProcessedLineResult processLine(String line, String cliCIF, String fileUploadId, String accountNumber) throws Exception;

    @Override
    public void process(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                        String uploadUser, String subserviceId, String subservice, String accountNumber) {
        log.info("Processing lines. Total lines: {}", lines.size());
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (String line : lines) {
            try {
                ProcessedLineResult result = processLine(line, cliCIF, fileUploadId, accountNumber);
                invMasterBatch.add(result.getInvoice());
                processedLines.add(result.getLineData());
                totalAmount = totalAmount.add(result.getTotalAmount());
                successLines++;
            } catch (Exception e) {
                log.error("Failed to process line: {}", line, e);
                processedLines.add(handleProcessingError());
                rejectedLines++;

            }
        }
        invMasterRepository.saveAll(invMasterBatch);

        sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
                subservice, accountNumber);

        basicIntrumentList = processedLines;
        log.info("Finished processing. Total inserted: Rows={}", invMasterBatch.size());
        log.info("Processing completed. Success: {}, Rejected: {}", successLines, rejectedLines);
    }

    protected Map<String, String> handleProcessingError() {
        return Map.of(
                "status", "0008",
                "amount", "0",
                "clientName", "",
                "accountNumber", ""
        );
    }



    private void sendPayment(int size, BigDecimal totalAmount, String fileUploadId,
                             String CIF, String orderId, String uploadUser, String subserviceId,
                             String subservice, String accoutNumber) {
        try {
            // Create message JSON to direct:SendPayment
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("size", size);
            paymentData.put("totalAmount", totalAmount);
            paymentData.put("fileUploadId", fileUploadId);
            paymentData.put("orderID", orderId);
            paymentData.put("uploadUser", uploadUser);
            paymentData.put("subserviceId", subserviceId);
            paymentData.put("subservice", subservice);
            paymentData.put("cif", CIF);
            paymentData.put("accountNumber", accoutNumber);


            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(paymentData);

            log.info("Sending JSON to Kafka: {}", jsonPayload);
            producerTemplate.sendBody("direct:SendPayment", jsonPayload);

        } catch (Exception e) {
            log.error("Error sending JSON to Kafka", e);
        }
    }
}
