package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

public record MapStopDto(
        String identifier,
        String code,
        String description,
        Double latitude,
        Double longitude
) {
}
