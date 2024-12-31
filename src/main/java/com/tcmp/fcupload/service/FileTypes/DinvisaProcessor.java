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
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class DinvisaProcessor extends AbstractFileProcessor {
    public DinvisaProcessor(ProducerTemplate producerTemplate, FileUtils fileUtils, InvMasterRepository invMasterRepository, CacheManager cacheManager) {
        super(producerTemplate, fileUtils, invMasterRepository, cacheManager);
    }

    @Override
    protected ProcessedLineResult processLine(String line, String clientCIF, String fileUploadId, String accountNumber) throws Exception {

        log.info("Enter processDinvVisaLine()");

        if (line.length() < 69) {
            log.warn("Line too short, skipping: {}", line);
            throw new Exception();
        }

        String cardNumber = line.substring(0, 16).trim(); // Número de tarjeta
        String fullName = line.substring(16, 46).trim(); // Nombre del propietario
        String idNumber = line.substring(46, 60).trim(); // Número de identificación
        String codeOptar = line.substring(60, 64).trim(); // Código Optar
        String reference = line.substring(64, 69).trim(); // Referencia


        String invoiceId = UUID.randomUUID().toString().toUpperCase();

        LocalDateTime now = LocalDateTime.now();
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        LocalDateTime expDate = now.plusMonths(1);
        Date dateExpDate = Date.from(expDate.atZone(ZoneId.systemDefault()).toInstant());

        CustomFields customFields = new CustomFields(line);

        InvMaster invoice = createInvoice(
                invoiceId,clientCIF, cardNumber,  fullName,  codeOptar,
                 idNumber,  reference,  date,  dateExpDate,  fileUploadId, customFields
        );

        Map<String, String> lineData = Map.of(
                "status", "0007",
                "cardNumber", cardNumber,
                "fullName", fullName,
                "idNumber", idNumber,
                "codeOptar", codeOptar
        );

        return new ProcessedLineResult(invoice, lineData, BigDecimal.ZERO);


    }

    private InvMaster createInvoice(
            String invoiceId, String clientCIF, String cardNumber, String fullName, String codeOptar,
            String idNumber, String reference,  Date date, Date expDate, String fileUploadId, CustomFields customFields
    ) {

        InvMaster master = new InvMaster();

        master.setInvoiceId(invoiceId);
        master.setClientCif(clientCIF);
        master.setServiceId(cardNumber);
        master.setCounterpart(fullName);
        master.setTotalAmount(BigDecimal.ZERO);
        master.setCurrency("USD");
        master.setPaymentMethod("Transferencia");
        master.setAccountType("DINVISA");
        master.setAccountCode(codeOptar);
        master.setIdType("ID");
        master.setIdCode(idNumber);
        master.setFullName(fullName);
        master.setDescription(reference);
        master.setStatus("NEW");
        master.setSubStatus("PENDING APPROVAL");
        master.setInvoiceDate(date);
        master.setExpirationDate(expDate);
        master.setSubject("Sub");
        master.setCategory(codeOptar);
        master.setUploaded(fileUploadId);
        master.setCustomFields(customFields.getFullLine());


        return master;
    }

}
