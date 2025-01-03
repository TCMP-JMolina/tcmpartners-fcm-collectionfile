//package com.tcmp.fcupload.service.FileTypes;

//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.opencsv.CSVReader;
//import com.opencsv.exceptions.CsvException;
//import com.tcmp.fcupload.dto.ProcessedLineResult;
//import com.tcmp.fcupload.model.InvMaster;
//import com.tcmp.fcupload.repository.InvMasterRepository;
//import com.tcmp.fcupload.utils.FileUtils;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.camel.Exchange;
//import org.apache.camel.ProducerTemplate;
//import org.springframework.cache.CacheManager;
//import org.springframework.stereotype.Component;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//
//@Slf4j
//@RequiredArgsConstructor
//@Component
//public class EasyCSVProcessor implements FileProcessor {
//
//    protected final ProducerTemplate producerTemplate;
//    protected final FileUtils fileUtils;
//    protected final InvMasterRepository invMasterRepository;
//    protected final CacheManager cacheManager;
//
//    protected List<InvMaster> invMasterBatch = new ArrayList<>();
//    protected List<Map<String, String>> processedLines = new ArrayList<>();
//    protected List<Map<String, String>> basicIntrumentList;
//    protected Integer rejectedLines = 0;
//    protected Integer successLines = 0;
//
//
//    @Override
//    public void process(List<String> lines, String fileUploadId, String cliCIF, String orderId,
//                        String uploadUser, String subserviceId, String subservice, String accountNumber) {
//        log.info("Processing processEasyPagosCSVFile");
//        String fileUploadId = exchange.getIn().getHeader("fileUploadId", String.class);
//        String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
//
//        try (InputStream inputStream = exchange.getIn().getBody(InputStream.class);
//             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//             CSVReader csvReader = new CSVReader(reader)) {
//
//            log.info("Processing CSV file: {}", fileName);
//
//            // Leer todas las líneas del archivo CSV
//            List<String[]> records = csvReader.readAll();
//
//            // Procesar cada línea (excluyendo la primera si es un encabezado)
//            for (int i = 1; i < records.size(); i++) {
//                String[] columns = records.get(i);
//
//                // Verificar que la línea tiene el número correcto de columnas
//                if (columns.length < 5) {
//                    log.error("CSV Line has missing columns, skipping line: {}", (Object) columns);
//                    continue;
//                }
//
//                processEasyPagosCSVLine(columns, fileUploadId, cliCIF);
//                successLines++;
//            }
//
//        } catch (IOException | CsvException e) {
//            log.error("Error processing CSV file: {}", fileName, e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    protected Map<String, String> handleProcessingError() {
//        return Map.of(
//                "status", "0008",
//                "amount", "0",
//                "clientName", "",
//                "accountNumber", ""
//        );
//    }
//
//
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
//
//}
