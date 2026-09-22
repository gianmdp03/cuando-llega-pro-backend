package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import java.util.List;

/** Consolidated live arrivals for every line serving one physical stop. */
public record StopArrivalsDto(
        String identifier,
        Double latitude,
        Double longitude,
        List<StopLineArrivalsDto> lines
) {
}
