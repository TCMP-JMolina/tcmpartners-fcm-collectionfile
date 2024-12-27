package com.tcmp.fcupload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileStatusUpdate {

    @JsonProperty("Status")
    @NotNull(message = "Status is required")
    @Pattern(regexp = "PROCESSING|COMPLETED|FAILED", message = "Invalid Status")
    private String Status;

    @JsonProperty("Substatus")
    @NotNull(message = "Substatus is required")
    @Pattern(regexp = "VALIDATING|PROCESSING|SUCCESS|ERROR", message = "Invalid Substatus")
    private String Substatus;

    @JsonProperty("Successfull")
    private Integer Successfull;

    @JsonProperty("Failed")
    private Integer Failed;

}