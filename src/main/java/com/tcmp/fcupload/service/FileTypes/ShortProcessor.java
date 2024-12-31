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
public class ShortProcessor extends AbstractFileProcessor {
    public ShortProcessor(ProducerTemplate producerTemplate, FileUtils fileUtils, InvMasterRepository invMasterRepository, CacheManager cacheManager) {
        super(producerTemplate, fileUtils, invMasterRepository, cacheManager);
    }

    @Override
    protected ProcessedLineResult processLine(String line, String clientCIF, String fileUploadId, String accountNumber) throws Exception {


        log.info("Processing SHORT_UEES Line: {}", line);

        String code = line.substring(0, 2).trim();
        String counterpart = line.substring(2, 22).trim();
        String currency = line.substring(22, 25).trim();
        BigDecimal amount = new BigDecimal(line.substring(25, 38).trim());
        String paymentMethod = line.substring(38, 41).trim();
        String accountType = line.substring(41, 44).trim();
        String accountNumberAux = line.substring(44, 64).trim();
        String reference = line.substring(64, 104).trim();
        String clientIdType = line.substring(104, 105).trim();
        String clientIdNumber = line.substring(105, 119).trim();
        String clientName = line.substring(119, 160).trim();
        String additionalField = line.substring(160, 181).trim();

        String billId = UUID.randomUUID().toString().toUpperCase();
        String invId = UUID.randomUUID().toString().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        LocalDateTime expDateTime = now.plusMonths(1);
        Date expDate = Date.from(expDateTime.atZone(ZoneId.systemDefault()).toInstant());

        CustomFields customFields = new CustomFields(line);

        InvMaster invoice = createInvoice(
                 invId,  clientCIF,  fileUploadId, customFields, code,
                 counterpart, amount,   currency,  paymentMethod,   accountNumberAux,
                 clientIdType,  clientIdNumber, clientName,  reference,date,expDate
        );

        Map<String, String> lineData = Map.of(
                "status", "0007",
                "amount", amount.toString(),
                "clientName", clientName,
                "accountNumber", accountNumberAux,
                "serviceId",code
        );


        return new ProcessedLineResult(invoice, lineData, amount);

    }

    private InvMaster createInvoice(
            String invId, String clientCIF, String fileUploadId, CustomFields customFields,String code,
            String counterpart, BigDecimal amount,  String currency, String paymentMethod,  String accountNumberAux,
            String clientIdType, String clientIdNumber,String clientName, String reference,Date date,Date expDate
    ) {

        InvMaster master = new InvMaster();

        master.setInvoiceId(invId);
        master.setClientCif(clientCIF);
        master.setServiceId(code);
        master.setCounterpart(counterpart);
        master.setTotalAmount(amount);
        master.setCurrency(currency);
        master.setPaymentMethod(paymentMethod);
        master.setAccountType("SHORT_UEES");
        master.setAccountCode(accountNumberAux);
        master.setIdType(clientIdType);
        master.setIdCode(clientIdNumber);
        master.setFullName(clientName);
        master.setDescription(reference);
        master.setStatus("NEW");
        master.setSubStatus("PENDING APPROVAL");
        master.setInvoiceDate(date);
        master.setExpirationDate(expDate);
        master.setSubject("Sub");
        master.setCategory(code);
        master.setUploaded(fileUploadId);
        master.setCustomFields(customFields.getFullLine());


        return master;
    }
}
