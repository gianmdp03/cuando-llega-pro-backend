package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitRouteResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing municipal transit catalog endpoints (lines, stops, and polyline routes).
 * Endpoints are protected by JWT authentication (inherited from SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/transit")
public class TransitCatalogController {

    private final TransitCatalogService transitCatalogService;

    public TransitCatalogController(TransitCatalogService transitCatalogService) {
        this.transitCatalogService = transitCatalogService;
    }

    /**
     * Retrieves the entire catalog of transit lines.
     *
     * @return 200 OK with list of TransitLineDTO objects
     */
    @GetMapping("/lines")
    public ResponseEntity<List<TransitLineDTO>> getLines() {
        List<TransitLineDTO> lines = transitCatalogService.getLines();
        return ResponseEntity.ok(lines);
    }

    /**
     * Retrieves all bus stops for a specific transit line.
     *
     * @param lineCode transit line identifier (e.g. "511", "522")
     * @return 200 OK with list of TransitStopDTO objects
     */
    @GetMapping("/lines/{lineCode}/stops")
    public ResponseEntity<List<TransitStopDTO>> getStops(@PathVariable("lineCode") String lineCode) {
        List<TransitStopDTO> stops = transitCatalogService.getStopsForLine(lineCode);
        return ResponseEntity.ok(stops);
    }

    /**
     * Retrieves the route trace and polyline vertices for a specific transit line.
     *
     * @param lineCode transit line identifier (e.g. "511", "522")
     * @return 200 OK with TransitRouteResponseDTO
     */
    @GetMapping("/lines/{lineCode}/route")
    public ResponseEntity<TransitRouteResponseDTO> getRoute(@PathVariable("lineCode") String lineCode) {
        TransitRouteResponseDTO route = transitCatalogService.getRouteTrace(lineCode);
        return ResponseEntity.ok(route);
    }
}
