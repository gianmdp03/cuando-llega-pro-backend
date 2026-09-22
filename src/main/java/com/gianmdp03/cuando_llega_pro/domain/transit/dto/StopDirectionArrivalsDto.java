package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;

import java.util.List;

/** Arrival predictions for one travel direction of a line at a physical stop. */
public record StopDirectionArrivalsDto(
        String direction,
        String expandedDirection,
        List<BusArrivalItemDTO> arrivals
) {
}
