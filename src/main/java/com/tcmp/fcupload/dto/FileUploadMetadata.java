package com.tcmp.fcupload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadMetadata {
    @JsonProperty("FileName")
    private String fileName;

    @JsonProperty("MD5")
    private String fileMD5;

    @JsonProperty("Records")
    private int fileRowCount;

    @JsonProperty("Size")
    private long fileSize;

    @JsonProperty("Source")
    private String source;

//    @JsonProperty("CustomFields")
//    private CustomFields customFields;

    @JsonProperty("Reference")
    private String reference;

    @JsonProperty("UserLoad")
    private String uplUserload;

    @JsonProperty("Service")
    private String uplService;
}
