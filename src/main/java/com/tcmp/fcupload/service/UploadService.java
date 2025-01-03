package com.tcmp.fcupload.service;

import java.io.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.tcmp.fcupload.dto.*;
import com.tcmp.fcupload.model.InvMaster;
import com.tcmp.fcupload.service.FileTypes.*;
import com.tcmp.fcupload.utils.FileUtils;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcmp.fcupload.repository.InvMasterRepository;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

    @Autowired
    BatchService batchService;
    @Autowired
    InstrumentService instrumentService;

    @Autowired
    private RestTemplate restTemplate;
    private final ProducerTemplate producerTemplate;

    @Value("${external.debtsServiceUrl}")
    private String debtsServiceUrl;

    private final InvMasterRepository invMasterRepository;
    Integer rejectedLines = 0;
    Integer successLines = 0;
    private final Map<String, List<String>> fileLinesCache = new ConcurrentHashMap<>();
    List<Map<String, String>> basicIntrumentList;


    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private CacheManager cacheManager;

//    @Autowired
//    private ProcessFileService processFileService;


    // public void processFile(Exchange exchange)  {
    public void processFile(Exchange exchange, Map<String, Object> blobMetadataRaw, String fileName, Long fileSize) throws IOException {

        long startTime = System.currentTimeMillis();
        Map<String, Object> blobMetadata = new HashMap<>();

        if (blobMetadataRaw != null) {
            for (Map.Entry<String, Object> entry : blobMetadataRaw.entrySet()) {
                String normalizedKey = entry.getKey().toLowerCase();
                blobMetadata.put(normalizedKey, entry.getValue());
            }
        }
        try (InputStream inputStream = exchange.getIn().getBody(InputStream.class)) {

            if (inputStream == null || inputStream.available() == 0) {
                log.warn("The InputStream is null.");
                return;
            }


            String extension = fileName != null ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase() : "unknown";


            InputStream clonedInputStream = cloneInputStream(inputStream);
            if (clonedInputStream.available() == 0) {
                log.warn("The input stream cloned is empty.");
                return;
            }
            String fileMD5 = generateMD5Checksum(inputStream);

            int fileRowCount =
                    (extension.equals("txt") || extension.equals("csv"))
                            ? countFileRows(clonedInputStream, fileName)
                            : -1;


            String fileUploadId = sendFileMetadataToService(
                    fileName, fileMD5, fileRowCount, fileSize, "RECAUDOS",
                    blobMetadata.get("uploaduser").toString(), blobMetadata.get("reference").toString(),
                    blobMetadata.get("service").toString());
            log.info("Metadata reading correctly");

            processLineByFileName(fileLinesCache, fileName, blobMetadata.get("cif").toString(), fileUploadId,
                    blobMetadata.get("interfacecode").toString(), blobMetadata.get("orderid").toString(),
                    blobMetadata.get("uploaduser").toString(), blobMetadata.get("subserviceid").toString(),
                    blobMetadata.get("subservice").toString(), blobMetadata.get("accountnumber").toString());


            successLines = (Integer) Objects.requireNonNull(Objects.requireNonNull(cacheManager.getCache("batchCache")).get("successLines")).get();
            rejectedLines = (Integer) Objects.requireNonNull(Objects.requireNonNull(cacheManager.getCache("batchCache")).get("rejectedLines")).get();

            log.info("Processing status SucessLines:{} and RejectedLines:{}", successLines, rejectedLines);

            updateFileStatus(fileUploadId, "PROCESSING", "VALIDATING", successLines, rejectedLines);


//            producerTemplate.sendBody(
//                    "direct:processFileAndSendToKafka",
//                    createBatch(fileName, fileMD5, fileRowCount, blobMetadata.get("cif").toString(), fileUploadId, blobMetadata));

            long endTime = System.currentTimeMillis();
            log.info("Time taken to process the file '{}' : {} ms", fileName, endTime - startTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void processLineByFileName(
            Map<String, List<String>> linesFile,
            String fileName,
            String cliCIF,
            String fileUploadId,
            String interfaceCode,
            String orderId,
            String uploadUser,
            String subserviceId,
            String subservice,
            String accountNumber
    ) throws Exception {

        List<String> lines = linesFile.get(fileName);
        if (lines != null && !lines.isEmpty()) {
//            if (fileName.toLowerCase().contains(".txt")) {
//                processFileService.processLineByLine(lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber);
//
//            }

            if (lines != null && !lines.isEmpty()) {
                if (fileName.toLowerCase().contains(".txt")) {
                    try {
                        FileProcessor strategy = getProcessorStrategy(interfaceCode);
                        strategy.process(lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber);
                    } catch (Exception e) {
                        log.error("Error processing line from file: {}", fileName, e);
                    }
                }
            }
//            if (fileName.toLowerCase().contains(".csv")) {
//
//                if (interfaceCode.toUpperCase().contains("EASYP_FULL_REC")) {
//                    try {
//                        processEasyPagosCSVFile(exchange, cliCIF);
//                    } catch (Exception e) {
//                        log.error("Error processing CSV file: {}", fileName, e);
//                    }
//                }
//
//            }
        }
    }


    private FileProcessor getProcessorStrategy(String interfaceCode) {
        switch (interfaceCode.toUpperCase()) {
            case "EASYP_FULL_REC":
                return new EasyFullProcessor(producerTemplate, fileUtils, invMasterRepository, cacheManager);
            case "EASYPAGOS_REC":
                return new EasyProcessor(producerTemplate, fileUtils, invMasterRepository, cacheManager);
            case "UNIV_REC":
                return new UnivProcessor(producerTemplate, fileUtils, invMasterRepository, cacheManager);
            case "DINVISA_REC":
                return new DinvisaProcessor(producerTemplate, fileUtils, invMasterRepository, cacheManager);
            case "SHORT_UEES_REC":
                return new ShortProcessor(producerTemplate, fileUtils, invMasterRepository, cacheManager);
            default:
                throw new UnsupportedOperationException("No processor found for interfaceCode: " + interfaceCode);
        }
    }

    private Batch createBatch(
            String fileName,
            String fileMD5,
            int fileRowCount,
            String clientCIF,
            String fileUploadId,
            Map<String, Object> metadata) {
        log.info("Success lines{}", successLines);

        instrumentService.createInstrument(metadata, basicIntrumentList);

        return Batch.builder()
                .batchId(fileUploadId)
                .referencia(metadata.get("reference").toString())
                .clientName(batchService.getClientName(clientCIF))
                .alternateName(batchService.getShortNameByClientId(clientCIF))
                .clientId(clientCIF)
                .type(metadata.get("producttype").toString())
                .typeId(metadata.get("subserviceid").toString())
                .createDate(LocalDate.now().toString())
                .startDate(LocalDate.now().toString())
                .endDate(metadata.get("effectiveenddate").toString())
                .executionDate(ZonedDateTime.now(ZoneId.of("America/Guayaquil")).toLocalDate().toString())
                .expirationDate(metadata.get("effectiveenddate").toString())
                .lastDate(LocalDate.now().plusDays(60).toString()) // consultar ultima modificacion
                .medio(metadata.get("uploadmedium").toString())
                .checksum(fileMD5)
                .customerAffiliationId(clientCIF)
                .customerUserId(metadata.get("uploaduser").toString())
                .customerProductId(metadata.get("subserviceid").toString())
                .fileName(fileName)
                .inputFile(null)
                .outputFile(null)
                .idStatus("idStatus") // Código pendiente**
                .status("status") // Descripción ***
                .totalItems(fileRowCount)
                .totalItemsRejected(rejectedLines)
                .totalItemsUploaded(successLines)
                .accountNumber(metadata.get("accountnumber").toString())
                .accountType(batchService.getClientTypeByCif(clientCIF)) // revisar
                .totalAmmount((float) (successLines / fileRowCount) * 100)
                .build();
    }

    public String sendFileMetadataToService(
            String fileName,
            String fileMD5,
            int fileRowCount,
            long fileSize,
            String source,
            String uploadUser,
            String reference,
            String service) {
        FileUploadMetadata metadata = new FileUploadMetadata(fileName, fileMD5, fileRowCount, fileSize, source, reference, uploadUser, service);
        String url = debtsServiceUrl + "/file/manager";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FileUploadMetadata> requestEntity = new HttpEntity<>(metadata, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String fileUploadId = response.getBody();
                log.info("File metadata sent successfully. Received File Upload ID: {}", fileUploadId);
                return extractIdFromJson(Objects.requireNonNull(response.getBody()));
            } else {
                log.error("Failed to send file metadata. Received status code: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send file metadata");
            }
        } catch (Exception e) {
            log.error("Error sending file metadata to service for file: {}", fileName, e);
            throw new RuntimeException("Error sending file metadata", e);
        }
    }

    private String extractIdFromJson(String jsonResponse) {
        return jsonResponse.replaceAll("[{}\"]", "").split(":")[1].trim();
    }

    public void updateFileStatus(String fileUploadId, String status, String substatus, Integer successLines, Integer rejectedLines) {
        if (status == null || substatus == null) {
            log.error("Error: 'status' or 'substatus' cannot be null. Status: {}, Substatus: {}", status, substatus);
            throw new IllegalArgumentException("Status and Substatus must be provided");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        FileStatusUpdate statusUpdate = new FileStatusUpdate(status, substatus, successLines, rejectedLines);
        String url = debtsServiceUrl + "/file/manager/" + fileUploadId;
        HttpEntity<FileStatusUpdate> requestEntity = new HttpEntity<>(statusUpdate, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
            log.info(response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("File status updated successfully for ID: " + fileUploadId);
            } else {
                log.error(
                        "Failed to update file status for ID: "
                                + fileUploadId
                                + ". Status code: "
                                + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error updating file status for ID: " + fileUploadId, e);
        }
    }

    public InputStream cloneInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            log.warn("The original InputStream is null.");
            return null;
        }

        inputStream.mark(Integer.MAX_VALUE);

        byte[] fileBytes = inputStream.readAllBytes();
        inputStream.reset();

        return new ByteArrayInputStream(fileBytes);
    }

    private String generateMD5Checksum(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }
            byte[] digestBytes = digest.digest();
            BigInteger bigInt = new BigInteger(1, digestBytes);
            return bigInt.toString(16).toUpperCase();
        } catch (Exception e) {
            log.error("Error generating MD5 checksum for file", e);
            return "";
        }
    }

    private int countFileRows(InputStream inputStream, String fileName) {
        int rowCount = 0;
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                rowCount++;
            }
            fileLinesCache.put(fileName, lines);
        } catch (Exception e) {
            log.error("Error counting rows in file: {}", fileName, e);
        }
        return rowCount;
    }


    // ARCHIVOS CSV --->

    @Async
    @Transactional
    public void processEasyPagosCSVFile(Exchange exchange, String cliCIF) {

        log.info("Processing processEasyPagosCSVFile");
        String fileUploadId = exchange.getIn().getHeader("fileUploadId", String.class);
        String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);

        try (InputStream inputStream = exchange.getIn().getBody(InputStream.class);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             CSVReader csvReader = new CSVReader(reader)) {

            log.info("Processing CSV file: {}", fileName);

            // Leer todas las líneas del archivo CSV
            List<String[]> records = csvReader.readAll();

            // Procesar cada línea (excluyendo la primera si es un encabezado)
            for (int i = 1; i < records.size(); i++) {
                String[] columns = records.get(i);

                // Verificar que la línea tiene el número correcto de columnas
                if (columns.length < 5) {
                    log.error("CSV Line has missing columns, skipping line: {}", (Object) columns);
                    continue;
                }

                processEasyPagosCSVLine(columns, fileUploadId, cliCIF);
                successLines++;
            }

        } catch (IOException | CsvException e) {
            log.error("Error processing CSV file: {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void processEasyPagosCSVLine(String[] columns, String fileUploadId, String cliCIF) {
        try {

            log.info("Processing processEasyPagosCSVLine");
            String invId = UUID.randomUUID().toString().toUpperCase(); // Generar ID de la factura
            String serviceId = columns[0].trim(); // ID del Servicio
            String invCpart = columns[1].trim(); // Contrapartida
            BigDecimal invTotal = new BigDecimal(columns[2].trim()); // Monto de la factura
            String invCcy = columns[3].trim(); // Moneda
            String invPMethod = columns[4].trim(); // Método de Pago
            String invDesc = columns.length > 5 ? columns[5].trim() : ""; // Descripción (opcional)
            String invIdCode = columns.length > 6 ? columns[6].trim() : ""; // Código de ID (opcional)
            String invFullName =
                    columns.length > 7 ? columns[7].trim() : ""; // Nombre del cliente (opcional)

            LocalDateTime localDateTime = LocalDateTime.now();
            Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
            LocalDateTime expDate = localDateTime.plusMonths(1);
            Date dateExpDate = Date.from(expDate.atZone(ZoneId.systemDefault()).toInstant());

            CustomFields customFields = new CustomFields(String.join(",", columns));

            saveInvoiceAndBill(invId, serviceId, invCpart, invTotal, invCcy, invPMethod, "SHORT_UEES",
                    invDesc, "ID", invIdCode, invFullName, invDesc, "NEW", "LOADED", "Sub",
                    serviceId, customFields, date, dateExpDate, fileUploadId, cliCIF);

        } catch (Exception e) {
            log.error("Failed to process CSV line: {}", (Object) columns, e);
            throw new RuntimeException(e);
        }
    }


    public void saveInvoiceAndBill(
            String invId,
            String serviceId,
            String invCpart,
            BigDecimal invTotal,
            String invCcy,
            String invPMethod,
            String invAccType,
            String invAccCode,
            String invIdType,
            String invIdCode,
            String invFullName,
            String invDesc,
            String invStatus,
            String invSubStatus,
            String invSubject,
            String invCategory,
            CustomFields customFields,
            Date date,
            Date expDate,
            String fileUploadId,
            String cliCIF) {

        InvMaster master = new InvMaster();

        master.setInvoiceId(invId);
        master.setClientCif(cliCIF);
        master.setServiceId(serviceId);
        master.setCounterpart(invCpart);
        master.setTotalAmount(invTotal);
        master.setCurrency(invCcy);
        master.setPaymentMethod(invPMethod);
        master.setAccountType(invAccType);
        master.setAccountCode(invAccCode);
        master.setIdType(invIdType);
        master.setIdCode(invIdCode);
        master.setFullName(invFullName);
        master.setDescription(invDesc);
        master.setStatus(invStatus);
        master.setSubStatus(invSubStatus);
        master.setInvoiceDate(date);
        master.setExpirationDate(expDate);
        master.setSubject(invSubject);
        master.setCategory(invCategory);
        master.setUploaded(fileUploadId);
        master.setCustomFields(customFields.getFullLine());

        invMasterRepository.save(master);

//        // Crear el objeto InvBiller (facturas activas)
//        String billId = UUID.randomUUID().toString().toUpperCase();
//        InvBiller invBiller = new InvBiller();
//        invBiller.setBillId(billId);
//        invBiller.setCliCIF(cliCIF);
//        invBiller.setServiceId(serviceId);
//        invBiller.setBillCpart(invCpart);
//        invBiller.setBillTotal(invTotal);
//        invBiller.setBillCcy(invCcy);
//        invBiller.setBillPMethod(invPMethod);
//        invBiller.setBillAccType(invAccType);
//        invBiller.setBillAccCode(invAccCode);
//        invBiller.setBillIdType(invIdType);
//        invBiller.setBillIdCode(invIdCode);
//        invBiller.setBillFullName(invFullName);
//        invBiller.setBillDesc(invDesc);
//        invBiller.setBillStatus(invStatus);
//        invBiller.setBillSubStatus(invSubStatus);
//        invBiller.setBillDate(date);
//        invBiller.setBillExpiration(expDate);
//        invBiller.setBillSubject(invSubject);
//        invBiller.setBillCategory(invCategory);
//        invBiller.setBillUpload(fileUploadId);
//        invBiller.setBillCustomFields(customFields.getFullLine());

//        invBillerRepository.save(invBiller);

        log.info("Inserted invoice and bill into DB for fileUploadId: {}", fileUploadId);
    }
}
