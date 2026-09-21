package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing an intersecting cross street on a transit line route.
 *
 * @param codigo      unique identifier of the intersecting street (e.g. "5625")
 * @param descripcion intersecting street name (e.g. "LEANDRO N. ALEM - MAR DEL PLATA")
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitIntersectionDTO(
        String codigo,
        String descripcion
) {}
