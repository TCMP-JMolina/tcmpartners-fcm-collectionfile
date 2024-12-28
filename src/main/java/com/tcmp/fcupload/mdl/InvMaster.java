package com.tcmp.fcupload.mdl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "FC_INVOICE_MST")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvMaster {

    @Id
    @Column(name = "INVID", nullable = false)
    private String invoiceId;

    @Column(name = "CLICIF")
    private String clientCif;

    @Column(name = "SERVICEID")
    private String serviceId;

    @Column(name = "INVCPART", length = 255)
    private String counterpart;

    @Column(name = "INVTOTAL", precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "INVCCY")
    private String currency;

    @Column(name = "INVPMETHOD")
    private String paymentMethod;

    @Column(name = "INVACCTYPE")
    private String accountType;

    @Column(name = "INVACCCODE")
    private String accountCode;

    @Column(name = "INVIDTYPE")
    private String idType;

    @Column(name = "INVIDCODE")
    private String idCode;

    @Column(name = "INVFULLNAME")
    private String fullName;

    @Column(name = "INVDESC")
    private String description;

    @Column(name = "INVSTATUS")
    private String status;

    @Column(name = "INVSUBSTATUS")
    private String subStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "INVDATE")
    private Date invoiceDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "INVEXPIRATION")
    private Date expirationDate;

    @Column(name = "INVSUBJECT", length = 255)
    private String subject;

    @Column(name = "INVCATEGORY", length = 255)
    private String category;

    @Column(name = "INVUPLOAD")
    private String uploaded;

    @Column(name = "INVCUSTOMFIELDS", length = 2000)
    private String customFields;
 
}
