package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import java.time.Instant;
import java.util.List;

/** Live arrivals for one commercial line at a physical stop. */
public record StopLineArrivalsDto(
        String lineCode,
        String status,
        Instant timestamp,
        Long deltaMinutes,
        List<StopDirectionArrivalsDto> directions,
        String error
) {
}
