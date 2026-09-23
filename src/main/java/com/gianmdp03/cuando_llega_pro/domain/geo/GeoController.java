package com.gianmdp03.cuando_llega_pro.domain.geo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/geo")
public class GeoController {

    private final ReverseGeocodingService reverseGeocodingService;

    public GeoController(ReverseGeocodingService reverseGeocodingService) {
        this.reverseGeocodingService = reverseGeocodingService;
    }

    @GetMapping("/reverse")
    public ResponseEntity<Map<String, String>> reverse(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        String formattedAddress = reverseGeocodingService.resolveAddress(latitude, longitude);
        return ResponseEntity.ok(Map.of("formattedAddress", formattedAddress));
    }
}
