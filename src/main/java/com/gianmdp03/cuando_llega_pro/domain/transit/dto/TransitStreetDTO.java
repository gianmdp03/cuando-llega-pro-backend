package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing a main street on a transit line route.
 *
 * @param codigo      unique identifier of the street (e.g. "5449")
 * @param descripcion street name and locality (e.g. "ALMAFUERTE - MAR DEL PLATA")
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitStreetDTO(
        String codigo,
        String descripcion
) {}
