package com.tcmp.fcupload.ctrl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCtrl {

    @GetMapping(value = "/health", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("AppName", "FILE COLLECTIONS MANAGER");
        response.put("appVer", "V1.1");
        response.put("status", "healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}