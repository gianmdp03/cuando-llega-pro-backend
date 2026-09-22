package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.MapLineDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopDetailDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.MapStopDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.DirectionDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.MapRouteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API used by the interactive map against the persisted catalogue.
 * The {@code /map} segment deliberately separates this local catalogue from
 * the legacy {@code /api/v1/transit/lines} endpoints backed by MGP.
 */
@RestController
@RequestMapping("/api/v1/transit/map")
@RequiredArgsConstructor
public class TransitController {

    private final TransitService transitService;

    @GetMapping("/lines")
    public ResponseEntity<List<MapLineDto>> getLines() {
        return ResponseEntity.ok(transitService.getLines());
    }

    @GetMapping("/lines/{lineCode}/directions")
    public ResponseEntity<List<DirectionDto>> getDirections(@PathVariable String lineCode) {
        return ResponseEntity.ok(transitService.getDirections(lineCode));
    }

    @GetMapping("/lines/{lineCode}/stops")
    public ResponseEntity<List<MapStopDto>> getStops(
            @PathVariable String lineCode,
            @RequestParam String direction
    ) {
        return ResponseEntity.ok(transitService.getStops(lineCode, direction));
    }

    @GetMapping("/lines/{lineCode}/routes")
    public ResponseEntity<List<MapRouteDto>> getRoutes(@PathVariable String lineCode) {
        return ResponseEntity.ok(transitService.getRoutes(lineCode));
    }

    @GetMapping("/stops/{identifier}")
    public ResponseEntity<StopDetailDto> getStop(@PathVariable String identifier) {
        return ResponseEntity.ok(transitService.getStop(identifier));
    }

}
