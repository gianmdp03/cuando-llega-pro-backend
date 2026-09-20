package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing a transit line.
 *
 * @param id          unique identifier or system key of the transit line
 * @param codigo      display/commercial line code (e.g. "511")
 * @param descripcion human-readable description of the line
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitLineDTO(
        String id,
        String codigo,
        String descripcion
) {}
