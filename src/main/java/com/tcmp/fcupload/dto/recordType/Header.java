package com.tcmp.fcupload.dto.recordType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Header {
    private String clientName;         // Client Name
    private String packageName;        // Package Name
    private String paymentCurrency;    // Payment Currency
    private String debitAccount;       // Debit Account
    private String transactionDate;    // Transaction Date
    private String pirReferenceNumber; // PIR Reference Number
}
