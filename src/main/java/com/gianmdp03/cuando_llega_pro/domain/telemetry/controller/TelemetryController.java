package com.gianmdp03.cuando_llega_pro.domain.telemetry.controller;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.ArrivalsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * REST controller providing ad-hoc transit arrival telemetry endpoints.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {

    private final ArrivalsService arrivalsService;

    public TelemetryController(ArrivalsService arrivalsService) {
        this.arrivalsService = arrivalsService;
    }

    /**
     * Retrieves upcoming arrivals for a given transit line and stop, optionally filtered by route variant (bandera).
     *
     * @param lineCode transit line identifier
     * @param stopId   transit stop identifier
     * @param bandera  optional route variant or branch
     * @return 200 OK with normalized ArrivalResponseDTO
     */
    @GetMapping("/arrivals")
    public ResponseEntity<ArrivalResponseDTO> getArrivals(
            @RequestParam String lineCode,
            @RequestParam String stopId,
            @RequestParam(required = false) String bandera
    ) {
        ArrivalResponseDTO response = arrivalsService.getArrivals(lineCode, stopId, bandera);
        return ResponseEntity.ok(response);
    }

    /** Persists a normalized, client-observed snapshot for the shared 15-second L1 cache and returns consolidated telemetry. */
    @PostMapping("/arrivals-cache")
    public ResponseEntity<ArrivalResponseDTO> refreshArrivalsCache(@RequestBody ArrivalResponseDTO snapshot) {
        ArrivalResponseDTO consolidated = arrivalsService.refreshFromClient(snapshot);
        return ResponseEntity.ok(consolidated);
    }
}
