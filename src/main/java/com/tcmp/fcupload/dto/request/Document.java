package com.tcmp.fcupload.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {

    @JacksonXmlProperty(localName = "sesInterSecu")
    private String sesInterSecu;

    @JacksonXmlProperty(localName = "cashTransPirIntNumb")
    private String cashTransPirIntNumb;

    @JacksonXmlProperty(localName = "cashTransIntNumb")
    private String cashTransIntNumb;

    @JacksonXmlProperty(localName = "cashTransRef")
    private String cashTransRef;

    @JacksonXmlProperty(localName = "pirIntNumb")
    private String pirIntNumb;

    @JacksonXmlProperty(localName = "transIntNumb")
    private String transIntNumb;

    @JacksonXmlProperty(localName = "transIntSubNumb")
    private String transIntSubNumb;

    @JacksonXmlProperty(localName = "pirTotIns")
    private int pirTotIns;

    @JacksonXmlProperty(localName = "pirTotAmnt")
    private String pirTotAmnt;

    @JacksonXmlProperty(localName = "debCredFlag")
    private String debCredFlag;

    @JacksonXmlProperty(localName = "reqDatTim")
    private String reqDatTim;

    @JacksonXmlProperty(localName = "payProdTyp")
    private String payProdTyp;

    @JacksonXmlProperty(localName = "payInstTyp")
    private String payInstTyp;

    @JacksonXmlProperty(localName = "instRef")
    private String instRef;

    @JacksonXmlProperty(localName = "instAmnt")
    private String instAmnt;

    @JacksonXmlProperty(localName = "instCurr")
    private String instCurr;

    @JacksonXmlProperty(localName = "cliCode")
    private String cliCode;

    @JacksonXmlProperty(localName = "cliNamBasLang")
    private String cliNamBasLang;


}
