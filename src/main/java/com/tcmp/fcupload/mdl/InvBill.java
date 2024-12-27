package com.tcmp.fcupload.mdl;

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
public class InvBill {

    @Id
    @Column(name = "BILLID", length = 36, nullable = false)
    private String billId;

    @Column(name = "CLICIF")
    private String cliCIF;

    @Column(name = "SERVICEID")
    private String serviceId;

    @Column(name = "BILLCPART")
    private String billCpart;

    @Column(name = "BILLTOTAL", precision = 19, scale = 2)
    private BigDecimal billTotal;

    @Column(name = "BILLCCY")
    private String billCcy;

    @Column(name = "BILLPMETHOD")
    private String billPMethod;

    @Column(name = "BILLACCTYPE")
    private String billAccType;

    @Column(name = "BILLACCCODE")
    private String billAccCode;

    @Column(name = "BILLIDTYPE")
    private String billIdType;

    @Column(name = "BILLIDCODE")
    private String billIdCode;

    @Column(name = "BILLFULLNAME")
    private String billFullName;

    @Column(name = "BILLDESC")
    private String billDesc;

    @Column(name = "BILLSTATUS")
    private String billStatus;

    @Column(name = "BILLSUBSTATUS")
    private String billSubStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "BILLDATE")
    private Date billDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "BILLEXPIRATION")
    private Date billExpiration;

    @Column(name = "BILLSUBJECT")
    private String billSubject;

    @Column(name = "BILLCATEGORY")
    private String billCategory;

    @Column(name = "BILLUPLOAD")
    private String billUpload;

    @Column(name = "BILLCUSTOMFIELDS")
    private String billCustomFields;

}
