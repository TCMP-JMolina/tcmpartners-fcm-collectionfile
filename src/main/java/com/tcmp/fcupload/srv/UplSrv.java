package com.tcmp.fcupload.srv;

import java.io.*;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.tcmp.fcupload.dto.*;
import com.tcmp.fcupload.srv.FileTypes.*;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcmp.fcupload.mdl.InvBill;
import com.tcmp.fcupload.mdl.InvRow;
import com.tcmp.fcupload.rep.InvBillRep;
import com.tcmp.fcupload.rep.InvRowRep;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class UplSrv {

    @Autowired
    BatchService batchService;
    @Autowired
    InstrumentService instrumentService;

    @Autowired
    private RestTemplate restTemplate;
    private final ProducerTemplate producerTemplate;

    @Value("${external.debtsServiceUrl}")
    private String debtsServiceUrl;

    private final InvBillRep invBillRep;
    private final InvRowRep invRowRep;

    // Configuración del tamaño del lote y del pool de hilos
    private final Map<String, List<String>> fileLinesCache = new ConcurrentHashMap<>();
    private Integer rejectedLines = 0;
    private Integer successLines = 0;
    List<Map<String, String>> basicIntrumentList;
    final int BATCH_SIZE = 1000; // Tamaño del lote
    List<InvRow> invRowBatch = new ArrayList<>();
    List<InvBill> invBillBatch = new ArrayList<>();
    List<Map<String, String>> processedLines = new ArrayList<>();

    public void processFile(Exchange exchange) {

        long startTime = System.currentTimeMillis();

        Map<String, Object> headers = exchange.getIn().getHeaders();
        Map<String, Object> blobMetadataRaw =
                exchange.getIn().getHeader("CamelAzureStorageBlobMetadata", Map.class);

        log.info("BlobMetadata with Raw " + blobMetadataRaw.values());

        Map<String, Object> blobMetadata = new HashMap<>();

        if (blobMetadataRaw != null) {
            for (Map.Entry<String, Object> entry : blobMetadataRaw.entrySet()) {
                String normalizedKey = entry.getKey().toLowerCase();
                blobMetadata.put(normalizedKey, entry.getValue());
            }
        }

        log.info("BlobMetadata with values: " + blobMetadata.values());

        String fileName = exchange.getIn().getHeader(BlobConstants.BLOB_NAME, String.class);

        String cliCIF = blobMetadata.get("cif").toString();

        Long fileSize = exchange.getIn().getHeader(BlobConstants.BLOB_SIZE, Long.class);
        if (fileSize == null) {
            fileSize = 0L;
            log.warn("Can not get the size, the size is going to be 0.");
        }
        log.info("File Size: {}", fileSize);

        String extension =
                fileName != null
                        ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                        : "unknown";

        try (InputStream inputStream = exchange.getIn().getBody(InputStream.class)) {

            if (inputStream == null || inputStream.available() == 0) {
                log.warn("El InputStream del archivo está vacío o nulo.");
                return;
            }


            // Clonar el InputStream para múltiples usos
            InputStream clonedInputStream = cloneInputStream(inputStream);

            // Validar que el archivo no está vacío
            if (clonedInputStream.available() == 0) {
                log.warn("El archivo proporcionado está vacío.");
                return;
            }

            String fileMD5 = generateMD5Checksum(inputStream);

            int fileRowCount =
                    (extension.equals("txt") || extension.equals("csv"))
                            ? countFileRows(clonedInputStream, fileName)
                            : -1;

            String fileUploadId =
                    sendFileMetadataToService(
                            fileName, fileMD5, fileRowCount, fileSize, "RECAUDOS"
                            , blobMetadata.get("uploaduser").toString(), blobMetadata.get("reference").toString(),
                            blobMetadata.get("service").toString());

            log.info("Metadata reading correctly");
            processLineByFileName(fileLinesCache, fileName, cliCIF, fileUploadId, exchange,
                    blobMetadata.get("interfacecode").toString(), blobMetadata.get("orderid").toString(),
                    blobMetadata.get("uploaduser").toString(), blobMetadata.get("subserviceid").toString(),
                    blobMetadata.get("subservice").toString(), blobMetadata.get("accountnumber").toString());

            // invBillRep.updateStatusAndSubStatusByCliCIF(cliCIF);
//
            updateFileStatus(fileUploadId, "PROCESSING", "VALIDATING", successLines, rejectedLines);

            producerTemplate.sendBody(
                    "direct:processFileAndSendToKafka",
                    createBatch(fileName, fileMD5, fileRowCount, cliCIF, fileUploadId, blobMetadata));

        } catch (Exception e) {
            log.error("Error processing file: " + fileName, e);
            updateFileStatus(
                    exchange.getIn().getHeader("fileUploadId", String.class), "FAILED", "ERROR", successLines, rejectedLines);
        }

        long endTime = System.currentTimeMillis();
        log.info("Time taken to process the file '{}' : {} ms", fileName, endTime - startTime);
    }

    private void processLineByFileName(
            Map<String, List<String>> linesFile, String fileName, String cliCIF, String fileUploadId,
            Exchange exchange, String interfaceCode, String orderId, String uploadUser, String subserviceId,
            String subservice, String accountNumber) {

        log.info("Enter processLineByFileName()");
        if (fileName.toLowerCase().contains(".txt")) {
            try {

                if (interfaceCode.toUpperCase().contains("EASYP_FULL_REC")) {
                    List<String> lines = linesFile.get(fileName);
                    if (lines != null && !lines.isEmpty()) {
                        processEasyPagosFullLine(
                                lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber); // Procesar todas las líneas
                    } else {
                        log.warn("No se encontraron líneas para el archivo: {}", fileName);
                    }
                } else if (interfaceCode.toUpperCase().contains("EASYPAGOS_REC")) {
                    List<String> lines = linesFile.get(fileName);
                    if (lines != null && !lines.isEmpty()) {
                        processEasyPagosLine(
                                lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber); // Procesar todas las líneas para Easypagos
                    } else {
                        log.warn("No se encontraron líneas para el archivo: {}", fileName);
                    }
                } else if (interfaceCode.toUpperCase().contains("UNIV_REC")) {
                    List<String> lines = linesFile.get(fileName);
                    if (lines != null && !lines.isEmpty()) {
                        processUnivLine(
                                lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber); // Procesar todas las líneas para Easypagos
                    } else {
                        log.warn("No se encontraron líneas para el archivo: {}", fileName);
                    }
                } else if (interfaceCode.toUpperCase().contains("DINVISA_REC")) {
                    List<String> lines = linesFile.get(fileName);
                    if (lines != null && !lines.isEmpty()) {
                        processDinvVisaLine(
                                lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber); // Procesar todas las líneas para Easypagos
                    } else {
                        log.warn("No se encontraron líneas para el archivo: {}", fileName);
                    }
                } else if (interfaceCode.toUpperCase().contains("SHORT_UEES_REC")) {
                    List<String> lines = linesFile.get(fileName);
                    if (lines != null && !lines.isEmpty()) {
                        processShortUeesLine(
                                lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber);
                    }
                } else {
                    log.warn("Formato de archivo desconocido. File named: {}", fileName);
                }

            } catch (Exception e) {
                log.error("Error processing line from file : {}", fileName, e);
            }
        }

        // process CSV

        if (fileName.toLowerCase().contains(".csv")) {

            if (interfaceCode.toUpperCase().contains("EASYP_FULL_REC")) {
                List<String> lines = linesFile.get(fileName);
                if (lines != null && !lines.isEmpty()) {
                    processEasyPagosCSVFile(exchange, cliCIF); // Procesar todas las líneas
                } else {
                    log.warn("No se encontraron líneas para el archivo: {}", fileName);
                }
            }
        }
    }

    private void processLineByFileName2(
            Map<String, List<String>> linesFile,
            String fileName,
            String cliCIF,
            String fileUploadId,
            Exchange exchange,
            String interfaceCode,
            String orderId,
            String uploadUser,
            String subserviceId,
            String subservice,
            String accountNumber
    ) {

        List<String> lines = linesFile.get(fileName);
        if (lines != null && !lines.isEmpty()) {
            if (fileName.toLowerCase().contains(".txt")) {
                try {
                    FileProcessor strategy = getProcessorStrategy(interfaceCode);
                    strategy.process(lines, fileUploadId, cliCIF, orderId, uploadUser, subserviceId, subservice, accountNumber);
                } catch (Exception e) {
                    log.error("Error processing line from file: {}", fileName, e);
                }
            }
            if (fileName.toLowerCase().contains(".csv")) {

                if (interfaceCode.toUpperCase().contains("EASYP_FULL_REC")) {
                    try {
                        processEasyPagosCSVFile(exchange, cliCIF);
                    } catch (Exception e) {
                        log.error("Error processing CSV file: {}", fileName, e);
                    }
                }

            }
        }
    }


    private FileProcessor getProcessorStrategy(String interfaceCode) {
        switch (interfaceCode.toUpperCase()) {
            case "EASYP_FULL_REC":
                return new EasyFullProcessor();
            case "EASYPAGOS_REC":
                return new EasyProcessor();
            case "UNIV_REC":
                return new UnivProcessor();
            case "DINVISA_REC":
                return new DinvisaProcessor();
            case "SHORT_UEES_REC":
                return new ShortProcessor();
            default:
                throw new UnsupportedOperationException("No processor found for interfaceCode: " + interfaceCode);
        }
    }

    private Batch createBatch(
            String fileName,
            String fileMD5,
            int fileRowCount,
            String cliCIF,
            String fileUploadId,
            Map<String, Object> metadata) {
        int successfulLines = (int) metadata.getOrDefault("successfulLines", 0);

        instrumentService.createInstrument(metadata, basicIntrumentList);

        return Batch.builder()
                .batchId(fileUploadId)
                .referencia(metadata.get("reference").toString())
                .clientName(batchService.getClientName(cliCIF)) // debe ir nombre de la empresa
                .alternateName(batchService.getShortNameByClientId(cliCIF))
                .clientId(cliCIF)
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
                .customerAffiliationId(cliCIF)
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
                .accountType(batchService.getClientTypeByCif(cliCIF)) // revisar
                .totalAmmount((float) (successfulLines / fileRowCount) * 100)
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
        FileUploadMetadata metadata =
                new FileUploadMetadata(fileName, fileMD5, fileRowCount, fileSize, source, reference, uploadUser, service);
        String url = debtsServiceUrl + "/file/manager";


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FileUploadMetadata> requestEntity = new HttpEntity<>(metadata, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String fileUploadId = response.getBody();
                log.info("File metadata sent successfully. Received File Upload ID: " + fileUploadId);
                return extractIdFromJson(response.getBody());
            } else {
                log.error(
                        "Failed to send file metadata. Received status code: " + response.getStatusCode());
                throw new RuntimeException("Failed to send file metadata");
            }
        } catch (Exception e) {
            log.error("Error sending file metadata to service for file: " + fileName, e);
            throw new RuntimeException("Error sending file metadata", e);
        }
    }

    private String extractIdFromJson(String jsonResponse) {
        return jsonResponse.replaceAll("[{}\"]", "").split(":")[1].trim();
    }

    public void updateFileStatus(String fileUploadId, String status, String substatus, Integer successLines, Integer rejectedLines) {
        if (status == null || substatus == null) {
            log.error(
                    "Error: 'status' or 'substatus' cannot be null. Status: {}, Substatus: {}",
                    status,
                    substatus);
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
            log.warn("El InputStream original es nulo.");
            return null;
        }
        byte[] fileBytes = inputStream.readAllBytes();

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

    @Async
    @Transactional
    public void processDinvVisaLine(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                                    String uploadUser, String subserviceId, String subservice, String accountNumber) {

        log.info("Processing DINERS Lines. Total lines: {}", lines.size());

        int batchCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<InvRow> invRowBatch = new ArrayList<>();
        List<InvBill> invBillBatch = new ArrayList<>();
        List<Map<String, String>> processedLines = new ArrayList<>();
        log.info("Enter processDinvVisaLine()");
        for (String line : lines) {
            Map<String, String> lineData = new HashMap<>();
            try {
                if (line.length() < 69) {
                    log.warn("Line too short, skipping: {}", line);
                    lineData.put("status", "0008");
                    lineData.put("cardNumber", "");
                    lineData.put("fullName", "");
                    lineData.put("idNumber", "");
                    processedLines.add(lineData);
                    rejectedLines++;
                    continue;
                }

                // Extraer campos de acuerdo a la ficha técnica
                String cardNumber = line.substring(0, 16).trim(); // Número de tarjeta
                String fullName = line.substring(16, 46).trim(); // Nombre del propietario
                String idNumber = line.substring(46, 60).trim(); // Número de identificación
                String codeOptar = line.substring(60, 64).trim(); // Código Optar
                String reference = line.substring(64, 69).trim(); // Referencia


                totalAmount = new BigDecimal("13.29").setScale(2, RoundingMode.HALF_UP);


                String invId = UUID.randomUUID().toString().toUpperCase();
                String billId = UUID.randomUUID().toString().toUpperCase();
                LocalDateTime now = LocalDateTime.now();
                Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
                LocalDateTime expDate = now.plusMonths(1);
                Date dateExpDate = Date.from(expDate.atZone(ZoneId.systemDefault()).toInstant());

                CustomFields customFields = new CustomFields(line);

                invRowBatch.add(createInvRow(invId, cliCIF, cardNumber, fullName, BigDecimal.ZERO, "USD",
                        "Transferencia", "DINVISA", codeOptar, "ID", idNumber, fullName, reference, "NEW",
                        "SENT TO BANK", date, dateExpDate, "Sub", codeOptar, fileUploadId, customFields));

//        invBillBatch.add(createInvBill(billId, cliCIF, cardNumber, fullName, BigDecimal.ZERO, "USD",
//                "Transferencia", "DINVISA", codeOptar, "ID", idNumber, fullName, reference, "NEW",
//                "LOADED", date, dateExpDate, "Sub", codeOptar, fileUploadId, customFields));

                // Guardar los datos procesados en el mapa
                lineData.put("status", "0007");
                lineData.put("cardNumber", cardNumber);
                lineData.put("fullName", fullName);
                lineData.put("idNumber", idNumber);
                lineData.put("codeOptar", codeOptar);

                successLines++;
            } catch (Exception e) {
                log.error("Failed to process DINERS Line: {}", line, e);
                lineData.put("status", "0008");
                lineData.put("cardNumber", "");
                lineData.put("fullName", "");
                lineData.put("idNumber", "");
                rejectedLines++;
            }

            processedLines.add(lineData);

            if (invRowBatch.size() >= BATCH_SIZE) {
                invRowRep.saveAll(invRowBatch);
                //invBillRep.saveAll(invBillBatch);
                log.info("Batch saved. Rows={}", invRowBatch.size());
                invRowBatch.clear();
                //invBillBatch.clear();
                batchCount++;
            }
        }

        if (!invRowBatch.isEmpty()) {
            invRowRep.saveAll(invRowBatch);
            log.info("Saving registers in Master BD");
            sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
                    subservice, accountNumber);

            //invBillRep.saveAll(invBillBatch);
            log.info("Final batch saved. Rows={}", invRowBatch.size());
        }
        basicIntrumentList = processedLines;
        log.info("Finished processing DINERS Lines. Total batches processed: {}", batchCount);
    }


    public void processShortUeesLine(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                                     String uploadUser, String subserviceId, String subservice, String accountNumber) {
        log.info("Processing SHORT_UEES Lines. Total lines: {}", lines.size());

        List<InvRow> invRowBatch = new ArrayList<>();
        List<InvBill> invBillBatch = new ArrayList<>();
        List<Map<String, String>> processedLines = new ArrayList<>();
        int batchCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (String line : lines) {
            Map<String, String> lineData = new HashMap<>();
            try {

                log.info("Processing SHORT_UEES Line: {}", line);
                String codigo = line.substring(0, 2).trim();
                String contrapartida = line.substring(2, 22).trim();
                String moneda = line.substring(22, 25).trim();
                BigDecimal valor = new BigDecimal(line.substring(25, 38).trim());
                String formaPago = line.substring(38, 41).trim();
                String tipoCuenta = line.substring(41, 44).trim();
                String numeroCuenta = line.substring(44, 64).trim();
                String referencia = line.substring(64, 104).trim();
                String tipoIdCliente = line.substring(104, 105).trim();
                String numeroIdCliente = line.substring(105, 119).trim();
                String nombreCliente = line.substring(119, 160).trim();
                String campoAdicional = line.substring(160, 181).trim();

                String billId = UUID.randomUUID().toString().toUpperCase();
                String invId = UUID.randomUUID().toString().toUpperCase();
                LocalDateTime now = LocalDateTime.now();
                Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
                LocalDateTime expDateTime = now.plusMonths(1);
                Date expDate = Date.from(expDateTime.atZone(ZoneId.systemDefault()).toInstant());

                CustomFields customFields = new CustomFields(line);

                // Crear y añadir el objeto InvRow al lote
                InvRow invoice = new InvRow();
                invoice.setInvId(invId);
                invoice.setCliCIF(cliCIF);
                invoice.setServiceId(codigo);
                invoice.setInvCpart(contrapartida);
                invoice.setInvTotal(valor);
                invoice.setInvCcy(moneda);
                invoice.setInvPMethod(formaPago);
                invoice.setInvAccType("SHORT_UEES");
                invoice.setInvAccCode(numeroCuenta);
                invoice.setInvIdType(tipoIdCliente);
                invoice.setInvIdCode(numeroIdCliente);
                invoice.setInvFullName(nombreCliente);
                invoice.setInvDesc(referencia);
                invoice.setInvStatus("NEW");
                invoice.setInvSubStatus("SENT TO BANK");
                invoice.setInvDate(date);
                invoice.setInvExpiration(expDate);
                invoice.setInvSubject("Sub");
                invoice.setInvCategory(codigo);
                invoice.setInvUpload(fileUploadId);
                invoice.setInvCustomFields(customFields.getFullLine());
                invRowBatch.add(invoice);

//        // Crear y añadir el objeto InvBill al lote
//        InvBill invBill = new InvBill();
//        invBill.setBillId(billId);
//        invBill.setCliCIF(cliCIF);
//        invBill.setServiceId(codigo);
//        invBill.setBillCpart(contrapartida);
//        invBill.setBillTotal(valor);
//        invBill.setBillCcy(moneda);
//        invBill.setBillPMethod(formaPago);
//        invBill.setBillAccType("SHORT_UEES");
//        invBill.setBillAccCode(numeroCuenta);
//        invBill.setBillIdType(tipoIdCliente);
//        invBill.setBillIdCode(numeroIdCliente);
//        invBill.setBillFullName(nombreCliente);
//        invBill.setBillDesc(referencia);
//        invBill.setBillStatus("NEW");
//        invBill.setBillSubStatus("LOADED");
//        invBill.setBillDate(date);
//        invBill.setBillExpiration(expDate);
//        invBill.setBillSubject("Sub");
//        invBill.setBillCategory(codigo);
//        invBill.setBillUpload(fileUploadId);
//        invBill.setBillCustomFields(customFields.getFullLine());
//        invBillBatch.add(invBill);

                // Guardar los datos procesados en el mapa
                lineData.put("status", "0007");
                lineData.put("amount", valor.toString());
                lineData.put("clientName", nombreCliente);
                lineData.put("accountNumber", numeroCuenta);
                lineData.put("serviceId", codigo);
                successLines++;
            } catch (Exception e) {
                lineData.put("status", "0008");
                lineData.put("amount", "0");
                lineData.put("clientName", "");
                lineData.put("accountNumber", "");
                log.error("Failed to process Short UEES line", e);
                rejectedLines++;
            }
            processedLines.add(lineData);
        }
        // Guardar en la base de datos en lote
        if (!invRowBatch.isEmpty()) {
            invRowRep.saveAll(invRowBatch);
        }
        sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
                subservice, accountNumber);
//    if (!invBillBatch.isEmpty()) {
//      invBillRep.saveAll(invBillBatch);
//    }

        basicIntrumentList = processedLines;
        log.info(
                "Finished processing SHORT_UEES Lines. Total inserted: Rows={}",
                invRowBatch.size());
    }

    @Async
    @Transactional
    public void processEasyPagosFullLine(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                                         String uploadUser, String subserviceId, String subservice, String accNumber) {
        log.info("Processing Easypagos Full Lines. Total lines: {}", lines.size());

        int batchCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (String line : lines) {
            Map<String, String> lineData = new HashMap<>();
            try {
                String[] columns = line.split("\\t");

                log.info("Processing Easypagos Full TXT columns: " + Arrays.toString(columns));

                // Validar y extraer los campos de cada columna
                String invId = UUID.randomUUID().toString().toUpperCase();
                String serviceCode = validateFieldLength(columns[0].trim(), 3);
                String companyAccount = validateFieldLength(columns[1].trim(), 20);
                String paymentSequential = validateFieldLength(columns[2].trim(), 7);
                String paymentVoucher = validateFieldLength(columns[3].trim(), 20);
                String counterpart = validateFieldLength(columns[4].trim(), 80);
                String currencyCode = validateFieldLength(columns[5].trim(), 3);
                BigDecimal paymentValue = formatBigDecimal(columns[6].trim());
                String paymentMethod = validateFieldLength(columns[7].trim(), 3);
                String bankCode = validateFieldLength(columns[8].trim(), 3);
                String accountType = columns.length > 9 ? validateFieldLength(columns[9].trim(), 2) : "";
                String accountNumber = columns.length > 10 ? validateFieldLength(columns[10].trim(), 20) : "";
                String clientIdType = validateFieldLength(columns[11].trim(), 1);
                String clientIdNumber = validateFieldLength(columns[12].trim(), 13);
                String clientFullName = validateFieldLength(columns[13].trim(), 100);
                String paymentReference = validateFieldLength(columns[18].trim(), 25);

                // Fechas de procesamiento
                LocalDateTime now = LocalDateTime.now();
                Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
                LocalDateTime expDateTime = now.plusMonths(1);
                Date expDate = Date.from(expDateTime.atZone(ZoneId.systemDefault()).toInstant());

                // Crear el objeto CustomFields con la línea completa
                CustomFields customFields = new CustomFields(line);

                // Crear y añadir el objeto InvRow al lote
                InvRow invoice = new InvRow();
                invoice.setInvId(invId);
                invoice.setCliCIF(cliCIF);
                invoice.setServiceId(serviceCode);
                invoice.setInvCpart(counterpart);
                invoice.setInvTotal(paymentValue);
                invoice.setInvCcy(currencyCode);
                invoice.setInvPMethod(paymentMethod);
                invoice.setInvAccType(accountType);
                invoice.setInvAccCode(accountNumber);
                invoice.setInvIdType(clientIdType);
                invoice.setInvIdCode(clientIdNumber);
                invoice.setInvFullName(clientFullName);
                invoice.setInvDesc(paymentReference);
                invoice.setInvStatus("NEW");
                invoice.setInvSubStatus("SENT TO BANK");
                invoice.setInvDate(date);
                invoice.setInvExpiration(expDate);
                invoice.setInvSubject("Sub");
                invoice.setInvCategory(paymentSequential);
                invoice.setInvUpload(fileUploadId);
                invoice.setInvCustomFields(customFields.getFullLine());
                invRowBatch.add(invoice);

//        // Crear y añadir el objeto InvBill al lote
//        InvBill invBill = new InvBill();
//        invBill.setBillId(UUID.randomUUID().toString().toUpperCase());
//        invBill.setCliCIF(cliCIF);
//        invBill.setServiceId(serviceCode);
//        invBill.setBillCpart(counterpart);
//        invBill.setBillTotal(paymentValue);
//        invBill.setBillCcy(currencyCode);
//        invBill.setBillPMethod(paymentMethod);
//        invBill.setBillAccType(accountType);
//        invBill.setBillAccCode(accountNumber);
//        invBill.setBillIdType(clientIdType);
//        invBill.setBillIdCode(clientIdNumber);
//        invBill.setBillFullName(clientFullName);
//        invBill.setBillDesc(paymentReference);
//        invBill.setBillStatus("NEW");
//        invBill.setBillSubStatus("LOADED");
//        invBill.setBillDate(date);
//        invBill.setBillExpiration(expDate);
//        invBill.setBillSubject("Sub");
//        invBill.setBillCategory(paymentSequential);
//        invBill.setBillUpload(fileUploadId);
//        invBill.setBillCustomFields(customFields.getFullLine());
//        invBillBatch.add(invBill);

                // Guardar los datos procesados en el mapa
                lineData.put("status", "0007");
                lineData.put("amount", paymentValue.toString());
                lineData.put("clientName", clientFullName);
                lineData.put("accountNumber", accountNumber);
                processedLines.add(lineData);

                successLines++;
            } catch (Exception e) {
                log.error("Failed to process Easypagos Full line: {}", line, e);
                lineData.put("status", "0008");
                lineData.put("amount", "0");
                lineData.put("clientName", "");
                lineData.put("accountNumber", "");
                processedLines.add(lineData);
                rejectedLines++;
            }
        }

        // Guardar los lotes en la base de datos
        invRowRep.saveAll(invRowBatch);
        //  invBillRep.saveAll(invBillBatch);

        sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
                subservice, accNumber);

        basicIntrumentList = processedLines;
        log.info(
                "Finished processing Easypagos Full Lines. Total inserted: Rows={}, Bills={}",
                invRowBatch.size(),
                invBillBatch.size());
    }

    /**
     * Valida la longitud de un campo de texto, truncándolo si excede el límite.
     */
    private String validateFieldLength(String field, int maxLength) {
        return field != null && field.length() > maxLength ? field.substring(0, maxLength) : field;
    }

    /**
     * Formatea un valor String en BigDecimal, removiendo separadores de miles y asegurando precisión.
     */
    private BigDecimal formatBigDecimal(String value) {
        return new BigDecimal(value.replace(",", "").trim());
    }


    @Async
    @Transactional
    public void processEasyPagosLine(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                                     String uploadUser, String subserviceId, String subservice, String accountNumber) {
        log.info("Processing EasyPagos Lines. Total lines: {}", lines.size());

        List<InvRow> invRowBatch = new ArrayList<>();
        List<InvBill> invBillBatch = new ArrayList<>();
        List<Map<String, String>> processedLines = new ArrayList<>();
        int batchCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (String line : lines) {
            Map<String, String> lineData = new HashMap<>();
            try {
                log.info("Processing EasyPagos Line: {}", line);

                // Extraer los campos según las posiciones especificadas
                String invCategory = line.substring(0, 2).trim(); // Código de Orientación (1-2)
                String invCpart = line.substring(2, 22).trim(); // Contrapartida (3-22)
                String invCcy = line.substring(22, 25).trim(); // Moneda (23-25)

                // Validar y parsear el valor numérico
                String rawInvTotal = line.substring(25, 38).trim(); // Valor (26-38)
                BigDecimal invTotal;
                try {
                    invTotal = new BigDecimal(rawInvTotal).movePointLeft(2); // Formatear el valor con 2 decimales
                } catch (NumberFormatException e) {
                    log.error("Invalid numeric value for 'Valor': {}. Line skipped.", rawInvTotal);
                    lineData.put("status", "0008");
                    lineData.put("amount", "0");
                    lineData.put("clientName", invCpart);
                    lineData.put("accountNumber", "");
                    processedLines.add(lineData);
                    continue;
                }

                String invPMethod = line.substring(38, 41).trim(); // Forma de Cobro/Pago (39-41)
                String invAccType = line.substring(41, 44).trim(); // Tipo de Cuenta (42-44)
                String invAccCode = line.substring(44, 64).trim(); // Número de Cuenta (45-64)
                String invDesc = line.substring(64, 104).trim(); // Referencia (65-104)
                String invIdType = line.substring(104, 105).trim(); // Tipo ID Cliente (105)
                String invIdCode = line.substring(105, 119).trim(); // Número ID Cliente (106-119)
                String invFullName = line.substring(119, 160).trim(); // Nombre del Cliente (120-160)

                // Crear el identificador único de la factura
                String invId = UUID.randomUUID().toString().toUpperCase();
                String billId = UUID.randomUUID().toString().toUpperCase();

                // Fechas de procesamiento
                LocalDateTime localDateTime = LocalDateTime.now();
                Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                LocalDateTime expDate = localDateTime.plusMonths(1);
                Date dateExpDate = Date.from(expDate.atZone(ZoneId.systemDefault()).toInstant());

                // Crear el objeto CustomFields con la línea completa
                CustomFields customFields = new CustomFields(line);

                // Crear y añadir el objeto InvRow al lote
                InvRow invoice = new InvRow();
                invoice.setInvId(invId);
                invoice.setCliCIF(cliCIF);
                invoice.setServiceId(invCpart);
                invoice.setInvCpart(invCpart);
                invoice.setInvTotal(invTotal);
                invoice.setInvCcy(invCcy);
                invoice.setInvPMethod(invPMethod);
                invoice.setInvAccType(invAccType);
                invoice.setInvAccCode(invAccCode);
                invoice.setInvIdType(invIdType);
                invoice.setInvIdCode(invIdCode);
                invoice.setInvFullName(invFullName);
                invoice.setInvDesc(invDesc);
                invoice.setInvStatus("NEW");
                invoice.setInvSubStatus("SENT TO BANK");
                invoice.setInvDate(date);
                invoice.setInvExpiration(dateExpDate);
                invoice.setInvSubject("Sub");
                invoice.setInvCategory(invCategory);
                invoice.setInvUpload(fileUploadId);
                invoice.setInvCustomFields(customFields.getFullLine());
                invRowBatch.add(invoice);


                // Guardar los datos procesados en el mapa
                lineData.put("status", "0007");
                lineData.put("amount", invTotal.toString());
                lineData.put("clientName", invFullName);
                lineData.put("accountNumber", invAccCode);
                processedLines.add(lineData);

                successLines++;
            } catch (Exception e) {
                log.error("Failed to process EasyPagos Line: {}", line, e);
                lineData.put("status", "0008");
                lineData.put("amount", "0");
                lineData.put("clientName", "");
                lineData.put("accountNumber", "");
                processedLines.add(lineData);
                rejectedLines++;
            }
        }

        // Guardar los lotes en la base de datos
        invRowRep.saveAll(invRowBatch);

        sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
                subservice, accountNumber);

//    invBillRep.saveAll(invBillBatch);

        basicIntrumentList = processedLines;
        log.info("Finished processing EasyPagos Lines. Total inserted: Rows={}", invRowBatch.size());


    }


    @Async
    @Transactional
    public void processUnivLine(List<String> lines, String fileUploadId, String cliCIF, String orderId,
                                String uploadUser, String subserviceId, String subservice, String accountNumber) {
        log.info("Processing UNIV Lines. Total lines: {}", lines.size());


        int batchCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (String line : lines) {
            Map<String, String> lineData = new HashMap<>();
            try {
                log.info("Processing UNIV Line: {}", line);

                // Validar longitud mínima de la línea
                if (line.length() < 74) {
                    log.warn("Line too short, skipping: {}", line);
                    lineData.put("status", "0008");
                    lineData.put("amount", "0");
                    lineData.put("clientName", "");
                    lineData.put("accountNumber", "");
                    processedLines.add(lineData);
                    rejectedLines++;
                    continue;
                }

                // Extraer los campos
                String preInvoiceNumber = line.substring(0, 13).trim();
                String name = line.substring(13, 43).trim();
                String clientIdNumber = line.substring(43, 53).trim();
                String rawAmount = line.substring(53, 62).trim();
                BigDecimal amount;

                try {
                    amount = new BigDecimal(rawAmount).movePointLeft(2);
                    log.info("");// Ajustar a 2 decimales
                    totalAmount = totalAmount.add(amount);
                } catch (NumberFormatException e) {
                    log.error("Invalid numeric value for 'Valor': {}. Line skipped.", rawAmount);
                    lineData.put("status", "0008");
                    lineData.put("amount", "0");
                    lineData.put("clientName", "");
                    lineData.put("accountNumber", "");
                    processedLines.add(lineData);
                    rejectedLines++;
                    continue;
                }

                String dateStr = line.substring(62, 70).trim();
                LocalDate transactionDate;
                try {
                    transactionDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
                } catch (Exception e) {
                    log.error("Invalid date format for 'Fecha': {}. Line skipped.", dateStr);
                    lineData.put("status", "0008");
                    lineData.put("amount", "0");
                    lineData.put("clientName", "");
                    lineData.put("accountNumber", "");
                    processedLines.add(lineData);
                    rejectedLines++;
                    continue;
                }

                String status = line.substring(70, 71).trim();
                String universityCode = line.length() > 71 ? line.substring(71, 74).trim() : "";

                // Crear identificadores únicos
                String invId = UUID.randomUUID().toString().toUpperCase();
                String billId = UUID.randomUUID().toString().toUpperCase();

                // Fechas de procesamiento
                Date date = Date.from(transactionDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                LocalDateTime expDateTime = transactionDate.plusMonths(1).atStartOfDay();
                Date expDate = Date.from(expDateTime.atZone(ZoneId.systemDefault()).toInstant());

                // Crear el objeto CustomFields con la línea completa
                CustomFields customFields = new CustomFields(line);

                // Crear y añadir el objeto InvRow al lote


                invRowBatch.add(createInvRow(invId, cliCIF, preInvoiceNumber, clientIdNumber, amount, "USD",
                        "Transferencia", "UNIV", universityCode, "ID", clientIdNumber, name, status + "|" + universityCode, "NEW",
                        "SENT TO BANK", date, expDate, "Sub", universityCode, fileUploadId, customFields));


                // Guardar los datos procesados en el mapa
                lineData.put("status", "0007");
                lineData.put("amount", amount.toString());
                lineData.put("clientName", name);
                lineData.put("accountNumber", clientIdNumber);
                lineData.put("universityCode", universityCode);

                successLines++;
            } catch (Exception e) {
                log.error("Failed to process UNIV Line: {}", line, e);
                rejectedLines++;
            }

            processedLines.add(lineData);

            if (invRowBatch.size() >= BATCH_SIZE) {
                invRowRep.saveAll(invRowBatch);
//        invBillRep.saveAll(invBillBatch);
                log.info("Batch saved. Rows={}", invRowBatch.size());
                invRowBatch.clear();
//        invBillBatch.clear();
                batchCount++;
            }
        }

        // Guardar los registros restantes
        if (!invRowBatch.isEmpty()) {
            invRowRep.saveAll(invRowBatch);
            log.info("Saving in the master");

            sendPayment(lines.size(), totalAmount, fileUploadId, cliCIF, orderId, uploadUser, subserviceId,
                    subservice, accountNumber);

//      invBillRep.saveAll(invBillBatch);
            log.info("Final batch saved. Rows={}", invRowBatch.size());
        }

        basicIntrumentList = processedLines;
        log.info("Finished processing UNIV Lines. Total batches processed: {}", batchCount);
    }

    private void sendPayment(int size, BigDecimal totalAmount, String fileUploadId,
                             String CIF, String orderId, String uploadUser, String subserviceId,
                             String subservice, String accoutNumber) {
        try {
            // Crear un mapa para representar el JSON
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

            // Convertir el mapa a JSON usando Jackson ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(paymentData);

            // Log para depuración
            log.info("Enviando JSON a Kafka: {}", jsonPayload);

            // Enviar a la cola Kafka usando el producerTemplate
            producerTemplate.sendBody("direct:SendPayment", jsonPayload);

            log.info("JSON enviado correctamente a la cola de Kafka");

        } catch (Exception e) {
            log.error("Error al enviar el JSON a Kafka", e);
        }
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

    // envio de data a Base de datos

    private InvRow createInvRow(String invId, String cliCIF, String serviceId, String invCpart, BigDecimal invTotal,
                                String invCcy, String invPMethod, String invAccType, String invAccCode, String invIdType,
                                String invIdCode, String invFullName, String invDesc, String invStatus,
                                String invSubStatus, Date invDate, Date invExpiration, String invSubject,
                                String invCategory, String fileUploadId, CustomFields customFields) {
        InvRow invRow = new InvRow();
        invRow.setInvId(invId);
        invRow.setCliCIF(cliCIF);
        invRow.setServiceId(serviceId);
        invRow.setInvCpart(invCpart);
        invRow.setInvTotal(invTotal);
        invRow.setInvCcy(invCcy);
        invRow.setInvPMethod(invPMethod);
        invRow.setInvAccType(invAccType);
        invRow.setInvAccCode(invAccCode);
        invRow.setInvIdType(invIdType);
        invRow.setInvIdCode(invIdCode);
        invRow.setInvFullName(invFullName);
        invRow.setInvDesc(invDesc);
        invRow.setInvStatus(invStatus);
        invRow.setInvSubStatus(invSubStatus);
        invRow.setInvDate(invDate);
        invRow.setInvExpiration(invExpiration);
        invRow.setInvSubject(invSubject);
        invRow.setInvCategory(invCategory);
        invRow.setInvUpload(fileUploadId);
        invRow.setInvCustomFields(customFields.getFullLine());
        return invRow;
    }

    private InvBill createInvBill(String billId, String cliCIF, String serviceId, String billCpart, BigDecimal billTotal,
                                  String billCcy, String billPMethod, String billAccType, String billAccCode,
                                  String billIdType, String billIdCode, String billFullName, String billDesc,
                                  String billStatus, String billSubStatus, Date billDate, Date billExpiration,
                                  String billSubject, String billCategory, String fileUploadId, CustomFields customFields) {
        InvBill invBill = new InvBill();
        invBill.setBillId(billId);
        invBill.setCliCIF(cliCIF);
        invBill.setServiceId(serviceId);
        invBill.setBillCpart(billCpart);
        invBill.setBillTotal(billTotal);
        invBill.setBillCcy(billCcy);
        invBill.setBillPMethod(billPMethod);
        invBill.setBillAccType(billAccType);
        invBill.setBillAccCode(billAccCode);
        invBill.setBillIdType(billIdType);
        invBill.setBillIdCode(billIdCode);
        invBill.setBillFullName(billFullName);
        invBill.setBillDesc(billDesc);
        invBill.setBillStatus(billStatus);
        invBill.setBillSubStatus(billSubStatus);
        invBill.setBillDate(billDate);
        invBill.setBillExpiration(billExpiration);
        invBill.setBillSubject(billSubject);
        invBill.setBillCategory(billCategory);
        invBill.setBillUpload(fileUploadId);
        invBill.setBillCustomFields(customFields.getFullLine());
        return invBill;
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

        // Crear el objeto InvRow (histórico de subidas)
        InvRow invoice = new InvRow();
        invoice.setInvId(invId);
        invoice.setCliCIF(cliCIF);
        invoice.setServiceId(serviceId);
        invoice.setInvCpart(invCpart);
        invoice.setInvTotal(invTotal);
        invoice.setInvCcy(invCcy);
        invoice.setInvPMethod(invPMethod);
        invoice.setInvAccType(invAccType);
        invoice.setInvAccCode(invAccCode);
        invoice.setInvIdType(invIdType);
        invoice.setInvIdCode(invIdCode);
        invoice.setInvFullName(invFullName);
        invoice.setInvDesc(invDesc);
        invoice.setInvStatus(invStatus);
        invoice.setInvSubStatus(invSubStatus);
        invoice.setInvDate(date);
        invoice.setInvExpiration(expDate);
        invoice.setInvSubject(invSubject);
        invoice.setInvCategory(invCategory);
        invoice.setInvUpload(fileUploadId);
        invoice.setInvCustomFields(customFields.getFullLine()); // Aquí se asigna el objeto customFields

        // Guardar en la tabla de historiales
        invRowRep.save(invoice);

        // Crear el objeto InvBill (facturas activas)
        String billId = UUID.randomUUID().toString().toUpperCase();
        InvBill invBill = new InvBill();
        invBill.setBillId(billId);
        invBill.setCliCIF(cliCIF);
        invBill.setServiceId(serviceId);
        invBill.setBillCpart(invCpart);
        invBill.setBillTotal(invTotal);
        invBill.setBillCcy(invCcy);
        invBill.setBillPMethod(invPMethod);
        invBill.setBillAccType(invAccType);
        invBill.setBillAccCode(invAccCode);
        invBill.setBillIdType(invIdType);
        invBill.setBillIdCode(invIdCode);
        invBill.setBillFullName(invFullName);
        invBill.setBillDesc(invDesc);
        invBill.setBillStatus(invStatus);
        invBill.setBillSubStatus(invSubStatus);
        invBill.setBillDate(date);
        invBill.setBillExpiration(expDate);
        invBill.setBillSubject(invSubject);
        invBill.setBillCategory(invCategory);
        invBill.setBillUpload(fileUploadId);
        invBill.setBillCustomFields(customFields.getFullLine());

        invBillRep.save(invBill);

        log.info("Inserted invoice and bill into DB for fileUploadId: {}", fileUploadId);
    }
}
