package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import java.util.List;

public record ParadaDetalleDTO(
        String identificador,
        Double latitud,
        Double longitud,
        List<LineaSentidoDTO> lineas
) {
}
