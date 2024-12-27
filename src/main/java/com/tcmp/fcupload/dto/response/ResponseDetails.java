package com.tcmp.fcupload.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseDetails {

    @JacksonXmlProperty(localName = "DebitRespStatCod")
    private String debitRespStatCod;

    @JacksonXmlProperty(localName = "DebitRespErrCod")
    private String debitRespErrCod;

    @JacksonXmlProperty(localName = "DebitRespErrMess")
    private String debitRespErrMess;

    @JacksonXmlProperty(localName = "DebitRespRefNumb")
    private String debitRespRefNumb;

    @JacksonXmlProperty(localName = "CreditRespStatCod")
    private String creditRespStatCod;

    @JacksonXmlProperty(localName = "CreditRespErrCod")
    private String creditRespErrCod;

    @JacksonXmlProperty(localName = "CreditRespErrMess")
    private String creditRespErrMess;

    @JacksonXmlProperty(localName = "CreditRespRefNumb")
    private String creditRespRefNumb;
}
