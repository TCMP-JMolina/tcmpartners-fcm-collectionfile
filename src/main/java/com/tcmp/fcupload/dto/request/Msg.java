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
public class Msg {

    @JacksonXmlProperty(localName = "Pmnt")
    private Pmnt pmnt;

    @JacksonXmlProperty(localName = "Extn")
    private Extn extn;
}