package com.tcmp.fcupload.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JacksonXmlRootElement(localName = "FndtMsg", namespace = "http://fundtech.com/SCL/CommonTypes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FndtMsg {
    @JacksonXmlProperty(localName = "Header")
    private Header header;

    @JacksonXmlProperty(localName = "Msg")
    private Msg msg;
}
