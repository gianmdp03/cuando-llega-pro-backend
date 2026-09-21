package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing a transit line.
 *
 * @param id             unique identifier or system key of the transit line (CodigoLineaParada)
 * @param codigo         display/commercial line code (e.g. "511")
 * @param descripcion    human-readable description of the line
 * @param codigoEntidad  upstream municipal entity code
 * @param codigoEmpresa  upstream municipal enterprise code
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitLineDTO(
        String id,
        String codigo,
        String descripcion,
        String codigoEntidad,
        Integer codigoEmpresa
) {
    public TransitLineDTO(String id, String codigo, String descripcion) {
        this(id, codigo, descripcion, null, null);
    }

    public TransitLineDTO(String id, String descripcion, String codigoEntidad, Integer codigoEmpresa) {
        this(id, descripcion, descripcion, codigoEntidad, codigoEmpresa);
    }
}
