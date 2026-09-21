package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

/** Line and direction served by a physical stop. */
public record LineaSentidoDTO(
        String codigoLinea,
        String nombreLinea,
        String bandera,
        String banderaAmpliada
) {
}
