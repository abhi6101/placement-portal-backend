package com.abhi.authProject.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static int pingCount = 0;
    private static LocalDateTime lastPing = LocalDateTime.now();

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        pingCount++;
        lastPing = LocalDateTime.now();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", lastPing.toString());
        response.put("pingCount", pingCount);
        response.put("message", "Server is alive and running");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/stats")
    public ResponseEntity<Map<String, Object>> healthStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPings", pingCount);
        stats.put("lastPing", lastPing.toString());
        stats.put("serverTime", LocalDateTime.now().toString());

        return ResponseEntity.ok(stats);
    }
}
