package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing a bus stop with geographic coordinates.
 *
 * @param id        stop identifier (e.g. "P3692")
 * @param nombre    stop name or label
 * @param calle     street name or intersection
 * @param latitude  GPS latitude
 * @param longitude GPS longitude
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitStopDTO(
        String id,
        String nombre,
        String calle,
        Double latitude,
        Double longitude
) {}
