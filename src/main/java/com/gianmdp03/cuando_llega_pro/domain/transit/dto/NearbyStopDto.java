package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import java.util.List;

/** A nearby physical stop with the lines and directions served there. */
public record NearbyStopDto(
        String identifier,
        String description,
        Double latitude,
        Double longitude,
        int distanceMeters,
        List<LineDirectionDto> directions
) {}
