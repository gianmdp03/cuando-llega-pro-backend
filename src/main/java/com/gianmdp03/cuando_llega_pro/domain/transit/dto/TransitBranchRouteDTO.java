package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Represents a specific branch (bandera) route trajectory.
 *
 * @param bandera     branch or variant code/abbreviation
 * @param descripcion branch description
 * @param points      list of rich route waypoints
 * @param coordinates list of [latitude, longitude] pairs for polyline rendering (Leaflet/MapLibre)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitBranchRouteDTO(
        String bandera,
        String descripcion,
        List<RoutePointDTO> points,
        List<List<Double>> coordinates
) {}
