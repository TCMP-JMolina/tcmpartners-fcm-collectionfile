package com.tcmp.fcupload.dto;

import com.tcmp.fcupload.model.InvMaster;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class ProcessedLineResult {
    private InvMaster invoice;
    private Map<String, String> lineData;
    private BigDecimal totalAmount;
}
