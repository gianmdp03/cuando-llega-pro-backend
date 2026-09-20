package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Geographic waypoint or vertex along a transit route.
 *
 * @param latitude    GPS latitude
 * @param longitude   GPS longitude
 * @param descripcion optional description or milestone name
 * @param isPuntoPaso whether this point is a formal transit stop/timing point
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutePointDTO(
        Double latitude,
        Double longitude,
        String descripcion,
        Boolean isPuntoPaso
) {}
