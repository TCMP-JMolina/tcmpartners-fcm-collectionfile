package com.tcmp.fcupload.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "FC_INVOICE_BILLS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvBiller {


    @Id
    @Column(name = "BILLID", length = 36, nullable = false)
    private String id;

    @Column(name = "CLICIF")
    private String clientCIF;

    @Column(name = "SERVICEID")
    private String serviceId;

    @Column(name = "BILLCPART")
    private String counterpart;

    @Column(name = "BILLTOTAL", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "BILLCCY")
    private String currency;

    @Column(name = "BILLPMETHOD")
    private String paymentMethod;

    @Column(name = "BILLACCTYPE")
    private String accountType;

    @Column(name = "BILLACCCODE")
    private String accountCode;

    @Column(name = "BILLIDTYPE")
    private String idType;

    @Column(name = "BILLIDCODE")
    private String idCode;

    @Column(name = "BILLFULLNAME")
    private String fullName;

    @Column(name = "BILLDESC")
    private String description;

    @Column(name = "BILLSTATUS")
    private String status;

    @Column(name = "BILLSUBSTATUS")
    private String subStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "BILLDATE")
    private Date date;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "BILLEXPIRATION")
    private Date expirationDate;

    @Column(name = "BILLSUBJECT")
    private String subject;

    @Column(name = "BILLCATEGORY")
    private String category;

    @Column(name = "BILLUPLOAD")
    private String uploadedFileId;

    @Column(name = "BILLCUSTOMFIELDS")
    private String customFields;

}
