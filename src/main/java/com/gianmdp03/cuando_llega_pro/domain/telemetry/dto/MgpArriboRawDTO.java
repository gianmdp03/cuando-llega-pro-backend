package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Direct DTO representation of upstream MGP arrivals feed with PascalCase JSON properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MgpArriboRawDTO(
        @JsonProperty("DescripcionLinea") @JsonAlias({"descripcionLinea", "linea", "line"}) String descripcionLinea,
        @JsonProperty("DescripcionBandera") @JsonAlias({"descripcionBandera", "bandera"}) String descripcionBandera,
        @JsonProperty("Arribo") @JsonAlias({"arribo"}) String arribo,
        @JsonProperty("Latitud") @JsonAlias({"latitud"}) String latitud,
        @JsonProperty("Longitud") @JsonAlias({"longitud"}) String longitud,
        @JsonProperty("LatitudParada") @JsonAlias({"latitudParada"}) String latitudParada,
        @JsonProperty("LongitudParada") @JsonAlias({"longitudParada"}) String longitudParada,
        @JsonProperty("EsAdaptado") @JsonAlias({"esAdaptado", "adaptado"}) String esAdaptado,
        @JsonProperty("IdentificadorCoche") @JsonAlias({"identificadorCoche", "coche"}) String identificadorCoche,
        @JsonProperty("UltimaFechaHoraGPS") @JsonAlias({"ultimaFechaHoraGps", "fechaHoraGps", "timestamp"}) String ultimaFechaHoraGps,
        @JsonProperty("CodigoLineaParada") @JsonAlias({"codigoLineaParada"}) String codigoLineaParada,
        @JsonProperty("DesvioHorario") @JsonAlias({"desvioHorario", "desvio"}) String desvioHorario,
        @JsonProperty("IdentificadorChofer") @JsonAlias({"identificadorChofer", "chofer"}) String identificadorChofer
) {

    public MgpArriboRaw toRaw() {
        return new MgpArriboRaw(
                descripcionLinea,
                descripcionBandera,
                arribo,
                latitud,
                longitud,
                latitudParada,
                longitudParada,
                esAdaptado,
                identificadorCoche,
                ultimaFechaHoraGps,
                codigoLineaParada,
                null,
                null,
                desvioHorario,
                identificadorChofer,
                null,
                null,
                null
        );
    }

    public static MgpArriboRawDTO fromRaw(MgpArriboRaw raw) {
        if (raw == null) {
            return null;
        }
        return new MgpArriboRawDTO(
                raw.descripcionLinea(),
                raw.descripcionBandera(),
                raw.arribo(),
                raw.latitud(),
                raw.longitud(),
                raw.latitudParada(),
                raw.longitudParada(),
                raw.esAdaptado(),
                raw.identificadorCoche(),
                raw.ultimaFechaHoraGps(),
                raw.codigoLineaParada(),
                raw.desvioHorario(),
                raw.identificadorChofer()
        );
    }
}
