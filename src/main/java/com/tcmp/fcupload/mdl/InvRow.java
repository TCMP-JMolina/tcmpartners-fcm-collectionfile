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
public class InvRow {
 

    @Id
    @Column(name = "INVID", length = 36, nullable = false)
    private String invId;

    @Column(name = "CLICIF", length = 100)
    private String cliCIF;

    @Column(name = "SERVICEID", length = 100)
    private String serviceId;

    @Column(name = "INVCPART", length = 200)
    private String invCpart;

    @Column(name = "INVTOTAL", precision = 19, scale = 2)
    private BigDecimal invTotal;

    @Column(name = "INVCCY", length = 20)
    private String invCcy;

    @Column(name = "INVPMETHOD", length = 100)
    private String invPMethod;

    @Column(name = "INVACCTYPE", length = 100)
    private String invAccType;

    @Column(name = "INVACCCODE", length = 100)
    private String invAccCode;

    @Column(name = "INVIDTYPE", length = 100)
    private String invIdType;

    @Column(name = "INVIDCODE", length = 100)
    private String invIdCode;

    @Column(name = "INVFULLNAME", length = 100)
    private String invFullName;

    @Column(name = "INVDESC", length = 100)
    private String invDesc;

    @Column(name = "INVSTATUS", length = 20)
    private String invStatus;

    @Column(name = "INVSUBSTATUS", length = 20)
    private String invSubStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "INVDATE")
    private Date invDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "INVEXPIRATION")
    private Date invExpiration;

    @Column(name = "INVSUBJECT", length = 100)
    private String invSubject;

    @Column(name = "INVCATEGORY", length = 50)
    private String invCategory;

    @Column(name = "INVUPLOAD", length = 36)
    private String invUpload;

    @Column(name = "INVCUSTOMFIELDS", length = 3000)
    private String invCustomFields;
 
}
