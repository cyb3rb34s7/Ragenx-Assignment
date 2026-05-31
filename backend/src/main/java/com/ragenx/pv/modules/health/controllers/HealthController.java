package com.ragenx.pv.modules.health.controllers;

import com.ragenx.pv.common.response.ApiResponse;
import com.ragenx.pv.common.response.ResponseFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Liveness endpoint (brief endpoint #5) and the first consumer of the common layer.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ResponseFactory.ok(Map.of("status", "UP"));
    }
}
