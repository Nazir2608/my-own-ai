package com.nazir.myownai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class HealthController {
    
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "OK", "message", "API is working!",
            "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/health")
    public Map<String, Object> healthRoot() {
        return Map.of(
            "status", "OK",
            "message", "Root API is working!",
            "timestamp", System.currentTimeMillis()
        );
    }
}
