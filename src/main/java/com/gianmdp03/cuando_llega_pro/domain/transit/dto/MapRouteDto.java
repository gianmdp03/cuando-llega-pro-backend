package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import java.util.List;

/** Official ordered geometry for a line branch, encoded as [longitude, latitude] pairs. */
public record MapRouteDto(
        String id,
        String branch,
        String description,
        List<List<Double>> coordinates
) {
}
