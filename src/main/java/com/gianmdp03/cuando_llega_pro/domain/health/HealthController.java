package com.gianmdp03.cuando_llega_pro.domain.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public health check REST controller.
 */
@RestController
public class HealthController {

    /**
     * Public liveness and health endpoint.
     *
     * @return 200 OK with UP status map
     */
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> healthz() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
