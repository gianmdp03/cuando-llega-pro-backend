package com.gianmdp03.cuando_llega_pro.domain.transit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data transfer object representing a bus stop with associated branch / destination variant (bandera).
 *
 * @param codigo                     internal municipal stop code (e.g. "17453")
 * @param identificador              public stop code (e.g. "P4031")
 * @param descripcion                stop description or street crossing text
 * @param abreviaturaBandera         short branch destination indicator (e.g. "A ACANTILADOS")
 * @param abreviaturaAmpliadaBandera extended branch destination indicator
 * @param latitudParada              optional latitude coordinate of the stop
 * @param longitudParada             optional longitude coordinate of the stop
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransitStopWithFlagDTO(
        String codigo,
        String identificador,
        String descripcion,
        String abreviaturaBandera,
        String abreviaturaAmpliadaBandera,
        Double latitudParada,
        Double longitudParada
) {}
