package com.tcmp.fcupload.service.FileTypes;

import com.tcmp.fcupload.dto.CustomFields;
import com.tcmp.fcupload.dto.ProcessedLineResult;
import com.tcmp.fcupload.model.InvMaster;
import com.tcmp.fcupload.repository.InvMasterRepository;
import com.tcmp.fcupload.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
public class EasyProcessor extends AbstractFileProcessor {

    public EasyProcessor(ProducerTemplate producerTemplate, FileUtils fileUtils, InvMasterRepository invMasterRepository, CacheManager cacheManager) {
        super(producerTemplate, fileUtils, invMasterRepository, cacheManager);
    }

    @Override
    protected ProcessedLineResult processLine(String line, String clientCIF, String fileUploadId, String accountNumber) throws Exception {

        log.info("Processing EasyPagos Line: {}", line);


        String orientationCode = line.substring(0, 2).trim(); // Código de Orientación (1-2)
        String counterpartCode = line.substring(2, 22).trim(); // Contrapartida (3-22)
        String currencyCode = line.substring(22, 25).trim(); // Moneda (23-25)

        String rawTotalAmount = line.substring(25, 38).trim(); // Valor (26-38)
        BigDecimal totalAmount = BigDecimal.ZERO;
        try {
            totalAmount = new BigDecimal(rawTotalAmount).movePointLeft(2);
        } catch (NumberFormatException e) {
            log.error("Invalid numeric value for 'Valor': {}. Line skipped.", totalAmount);
            throw new Exception();
        }

        String paymentMethod = line.substring(38, 41).trim(); // Forma de Cobro/Pago (39-41)
        String accountType = line.substring(41, 44).trim(); // Tipo de Cuenta (42-44)
        String accountNumberCode = line.substring(44, 64).trim(); // Número de Cuenta (45-64)
        String reference = line.substring(64, 104).trim(); // Referencia (65-104)
        String clientIdType = line.substring(104, 105).trim(); // Tipo ID Cliente (105)
        String clientIdCode = line.substring(105, 119).trim(); // Número ID Cliente (106-119)
        String clientFullName = line.substring(119, 160).trim(); // Nombre del Cliente (120-160)

        String invoiceId = UUID.randomUUID().toString().toUpperCase();

        LocalDateTime currentDateTime = LocalDateTime.now();
        Date createdDate = Date.from(currentDateTime.atZone(ZoneId.systemDefault()).toInstant());
        LocalDateTime expirationDateTime = currentDateTime.plusMonths(1);
        Date expirationDate = Date.from(expirationDateTime.atZone(ZoneId.systemDefault()).toInstant());

        CustomFields customFields = new CustomFields(line);

        InvMaster invoice = createInvoice(
                invoiceId, clientCIF, fileUploadId, counterpartCode, totalAmount,
                currencyCode, paymentMethod, accountType, accountNumberCode, clientIdType,
                clientIdCode, clientFullName, reference, orientationCode,
                createdDate, expirationDate, customFields
        );

        Map<String, String> lineData = Map.of(
                "status", "0007",
                "amount", totalAmount.toString(),
                "clientName", clientFullName,
                "accountNumber", accountNumberCode
        );

        return new ProcessedLineResult(invoice, lineData, totalAmount);
    }

    private InvMaster createInvoice(
            String invoiceId, String clientCIF, String fileUploadId, String counterpartCode, BigDecimal totalAmount,
            String currencyCode, String paymentMethod, String accountType, String accountNumberCode, String clientIdType,
            String clientIdCode, String clientFullName, String reference, String orientationCode,
            Date createdDate, Date expirationDate, CustomFields customFields
    ) {

        InvMaster master = new InvMaster();

        master.setInvoiceId(invoiceId);
        master.setClientCif(clientCIF);
        master.setServiceId(counterpartCode);
        master.setCounterpart(counterpartCode);
        master.setTotalAmount(totalAmount);
        master.setCurrency(currencyCode);
        master.setPaymentMethod(paymentMethod);
        master.setAccountType(accountType);
        master.setAccountCode(accountNumberCode);
        master.setIdType(clientIdType);
        master.setIdCode(clientIdCode);
        master.setFullName(clientFullName);
        master.setDescription(reference);
        master.setStatus("NEW");
        master.setSubStatus("PENDING APPROVAL");
        master.setInvoiceDate(createdDate);
        master.setExpirationDate(expirationDate);
        master.setSubject("Sub");
        master.setCategory(orientationCode);
        master.setUploaded(fileUploadId);
        master.setCustomFields(customFields.getFullLine());

        return master;
    }


}
