package com.tcmp.fcupload.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class FileUtils {

    public String validateFieldLength(String field, int maxLength) {
        return field != null && field.length() > maxLength ? field.substring(0, maxLength) : field;
    }


    public BigDecimal formatBigDecimal(String value) {
        return new BigDecimal(value.replace(",", "").trim());
    }
}
