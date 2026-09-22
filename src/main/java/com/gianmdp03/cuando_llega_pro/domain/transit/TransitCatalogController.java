package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitIntersectionDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitLineDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStopWithFlagDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.TransitStreetDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing municipal transit catalog endpoints (lines, streets, intersections, stops, routes).
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
     * Action: RecuperarLineaPorCuandoLlega
     */
    @GetMapping("/lines")
    public ResponseEntity<List<TransitLineDTO>> getLines() {
        return ResponseEntity.ok(transitCatalogService.getLines());
    }

    /**
     * Action: RecuperarCallesPrincipalPorLinea
     */
    @GetMapping("/lines/{lineCode}/streets")
    public ResponseEntity<List<TransitStreetDTO>> getMainStreets(@PathVariable("lineCode") String lineCode) {
        return ResponseEntity.ok(transitCatalogService.getMainStreetsByLine(lineCode));
    }

    /**
     * Action: RecuperarInterseccionPorLineaYCalle
     */
    @GetMapping("/lines/{lineCode}/streets/{streetCode}/intersections")
    public ResponseEntity<List<TransitIntersectionDTO>> getIntersections(
            @PathVariable("lineCode") String lineCode,
            @PathVariable("streetCode") String streetCode
    ) {
        return ResponseEntity.ok(transitCatalogService.getIntersectionsByLineAndStreet(lineCode, streetCode));
    }

    /**
     * Action: RecuperarParadasConBanderaPorLineaCalleEInterseccion
     */
    @GetMapping("/lines/{lineCode}/streets/{streetCode}/intersections/{intersectionCode}/stops")
    public ResponseEntity<List<TransitStopWithFlagDTO>> getStopsWithFlag(
            @PathVariable("lineCode") String lineCode,
            @PathVariable("streetCode") String streetCode,
            @PathVariable("intersectionCode") String intersectionCode
    ) {
        return ResponseEntity.ok(transitCatalogService.getStopsWithFlag(lineCode, streetCode, intersectionCode));
    }

}
