package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Route trace response containing branches and vertices for map visualization.
 *
 * @param lineCode    line identifier
 * @param branches    route branches (banderas)
 * @param allPoints   flattened list of all points
 * @param coordinates flattened list of all [lat, lng] coordinates
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitRouteResponseDTO(
        String lineCode,
        List<TransitBranchRouteDTO> branches,
        List<RoutePointDTO> allPoints,
        List<List<Double>> coordinates
) {}
