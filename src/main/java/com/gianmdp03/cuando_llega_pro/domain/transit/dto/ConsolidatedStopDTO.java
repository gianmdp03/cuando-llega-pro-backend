package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsolidatedStopDTO(
        String stopId,
        String calle,
        String interseccion,
        List<String> banderas
) {}
