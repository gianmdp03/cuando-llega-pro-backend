package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw municipal transit arrival item DTO mapping upstream PascalCase JSON keys.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MgpArriboRaw(
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
        @JsonProperty("distancia") @JsonAlias({"Distancia", "distanciaMetros"}) Integer distancia,
        @JsonProperty("minutos") @JsonAlias({"Minutos"}) Integer minutos
) {

    /**
     * Standard 11-argument constructor matching the upstream MGP schema specification.
     */
    public MgpArriboRaw(
            String descripcionLinea,
            String descripcionBandera,
            String arribo,
            String latitud,
            String longitud,
            String latitudParada,
            String longitudParada,
            String esAdaptado,
            String identificadorCoche,
            String ultimaFechaHoraGps,
            String codigoLineaParada
    ) {
        this(descripcionLinea, descripcionBandera, arribo, latitud, longitud,
                latitudParada, longitudParada, esAdaptado, identificadorCoche, ultimaFechaHoraGps, codigoLineaParada, null, null);
    }
}
