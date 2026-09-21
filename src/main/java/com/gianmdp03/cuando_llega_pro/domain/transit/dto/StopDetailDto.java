package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import java.util.List;

public record StopDetailDto(
        String identifier,
        Double latitude,
        Double longitude,
        List<LineDirectionDto> directions
) {
}
