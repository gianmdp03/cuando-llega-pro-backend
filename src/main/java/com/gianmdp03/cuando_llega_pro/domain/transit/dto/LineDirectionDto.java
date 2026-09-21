package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

/** Line and direction served by a physical stop. */
public record LineDirectionDto(
        String codeTransitLine,
        String nameTransitLine,
        String direction,
        String expandedDirection
) {
}
