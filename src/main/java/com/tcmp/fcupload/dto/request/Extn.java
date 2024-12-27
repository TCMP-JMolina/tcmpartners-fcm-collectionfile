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
public class Extn {

    @JacksonXmlProperty(localName = "addInf1")
    private String addInf1;

    @JacksonXmlProperty(localName = "addInf2")
    private String addInf2;

    @JacksonXmlProperty(localName = "addInf3")
    private String addInf3;

    @JacksonXmlProperty(localName = "addInf4")
    private String addInf4;

    @JacksonXmlProperty(localName = "addInf5")
    private String addInf5;

    @JacksonXmlProperty(localName = "chanlCod")
    private String chanlCod;

    @JacksonXmlProperty(localName = "pirRefNumb")
    private String pirReferenceNumber;

    @JacksonXmlProperty(localName = "chargeTo")
    private String chargeTo;

    @JacksonXmlProperty(localName = "creditBankIDCode")
    private String creditBankIDCode;

    @JacksonXmlProperty(localName = "creditBankName")
    private String creditBankName;

    @JacksonXmlProperty(localName = "beneNameBaseLang")
    private String beneNameBaseLang;

    @JacksonXmlProperty(localName = "registrationId")
    private String registrationId;

    @JacksonXmlProperty(localName = "clientGcisID")
    private String clientGcisID;

}
