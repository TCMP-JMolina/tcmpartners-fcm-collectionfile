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
public class Msg {

    @JacksonXmlProperty(localName = "TransIntNumb")
    private String transIntNumb;

    @JacksonXmlProperty(localName = "TransIntSubNumb")
    private String transIntSubNumb;

    @JacksonXmlProperty(localName = "IntrAmnt")
    private String intrAmnt;
}
