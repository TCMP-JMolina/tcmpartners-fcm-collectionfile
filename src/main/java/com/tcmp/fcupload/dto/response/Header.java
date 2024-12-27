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
public class Header {

    @JacksonXmlProperty(localName = "ReqExecId")
    private String reqExecId;

    @JacksonXmlProperty(localName = "ReqEvenCod")
    private String reqEvenCod;

    @JacksonXmlProperty(localName = "PirIntNumb")
    private String pirIntNumb;
}
