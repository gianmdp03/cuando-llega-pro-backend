package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

public record ParadaMapaDTO(
        String identificador,
        String codigo,
        String descripcion,
        Double latitud,
        Double longitud
) {
}
