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
public class EasyFullProcessor extends AbstractFileProcessor {

    public EasyFullProcessor(ProducerTemplate producerTemplate, FileUtils fileUtils, InvMasterRepository invMasterRepository, CacheManager cacheManager) {
        super(producerTemplate, fileUtils, invMasterRepository, cacheManager);
    }

    @Override
    protected ProcessedLineResult processLine(String line, String cliCIF, String fileUploadId, String accountNumber) {
        String[] columns = line.split("\\t");

        log.info("Processing Easypagos Full TXT columns: {}", Arrays.toString(columns));


        String invId = UUID.randomUUID().toString().toUpperCase();
        String serviceCode = fileUtils.validateFieldLength(columns[0].trim(), 3);
        String companyAccount = fileUtils.validateFieldLength(columns[1].trim(), 20);
        String paymentSequential = fileUtils.validateFieldLength(columns[2].trim(), 7);
        String paymentVoucher = fileUtils.validateFieldLength(columns[3].trim(), 20);
        String counterpart = fileUtils.validateFieldLength(columns[4].trim(), 80);
        String currencyCode = fileUtils.validateFieldLength(columns[5].trim(), 3);
        BigDecimal paymentValue = fileUtils.formatBigDecimal(columns[6].trim());
        String paymentMethod = fileUtils.validateFieldLength(columns[7].trim(), 3);
        String bankCode = fileUtils.validateFieldLength(columns[8].trim(), 3);
        String accountType = columns.length > 9 ? fileUtils.validateFieldLength(columns[9].trim(), 2) : "";
        String accountNumberAux = columns.length > 10 ? fileUtils.validateFieldLength(columns[10].trim(), 20) : "";
        String clientIdType = fileUtils.validateFieldLength(columns[11].trim(), 1);
        String clientIdNumber = fileUtils.validateFieldLength(columns[12].trim(), 13);
        String clientFullName = fileUtils.validateFieldLength(columns[13].trim(), 100);
        String paymentReference = fileUtils.validateFieldLength(columns[18].trim(), 25);

        CustomFields customFields = new CustomFields(line);

        LocalDateTime now = LocalDateTime.now();
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date expDate = Date.from(now.plusMonths(1).atZone(ZoneId.systemDefault()).toInstant());

        InvMaster master = createInvoice(
                invId,cliCIF, fileUploadId,  accountNumberAux, serviceCode,companyAccount, paymentSequential,
                paymentVoucher,counterpart, paymentValue, currencyCode,  paymentMethod, bankCode,accountType,
                clientIdType, clientIdNumber, clientFullName,paymentReference, date, expDate, customFields
        );


        Map<String, String> lineData = Map.of(
                "status", "0007",
                "amount", paymentValue.toString(),
                "clientName", clientFullName,
                "accountNumber", accountNumber
        );

        return new ProcessedLineResult(master, lineData, paymentValue);
    }



    private InvMaster createInvoice(
            String invId, String cliCIF,String fileUploadId, String accountNumberAux, String serviceCode,
            String companyAccount, String paymentSequential, String paymentVoucher, String counterpart,BigDecimal paymentValue,
            String currencyCode, String paymentMethod, String bankCode, String accountType,
            String clientIdType, String clientIdNumber, String clientFullName, String paymentReference,
            Date date, Date expDate, CustomFields customFields
    ) {


        InvMaster master = new InvMaster();

        master.setInvoiceId(invId);
        master.setClientCif(cliCIF);
        master.setServiceId(serviceCode);
        master.setCounterpart(counterpart);
        master.setTotalAmount(paymentValue);
        master.setCurrency(currencyCode);
        master.setPaymentMethod(paymentMethod);
        master.setAccountType(accountType);
        master.setAccountCode(accountNumberAux);
        master.setIdType(clientIdType);
        master.setIdCode(clientIdNumber);
        master.setFullName(clientFullName);
        master.setDescription(paymentReference);
        master.setStatus("NEW");
        master.setSubStatus("PENDING APPROVAL");
        master.setInvoiceDate(date);
        master.setExpirationDate(expDate);
        master.setSubject("Sub");
        master.setCategory(paymentSequential);
        master.setUploaded(fileUploadId);
        master.setCustomFields(customFields.getFullLine());


        return master;
    }

}