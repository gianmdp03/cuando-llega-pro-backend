package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw municipal transit arrival item DTO mapping upstream PascalCase JSON keys
 * matching real HAR contracts.
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
        @JsonProperty("minutos") @JsonAlias({"Minutos"}) Integer minutos,
        @JsonProperty("DesvioHorario") @JsonAlias({"desvioHorario", "desvio"}) String desvioHorario,
        @JsonProperty("IdentificadorChofer") @JsonAlias({"identificadorChofer", "chofer"}) String identificadorChofer,
        @JsonProperty("DescripcionCortaBandera") @JsonAlias({"descripcionCortaBandera"}) String descripcionCortaBandera,
        @JsonProperty("DescripcionCartelBandera") @JsonAlias({"descripcionCartelBandera"}) String descripcionCartelBandera,
        @JsonProperty("MensajeError") @JsonAlias({"mensajeError"}) String mensajeError
) {

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
                latitudParada, longitudParada, esAdaptado, identificadorCoche, ultimaFechaHoraGps,
                codigoLineaParada, null, null, null, null, null, null, null);
    }

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
            String codigoLineaParada,
            Integer distancia,
            Integer minutos
    ) {
        this(descripcionLinea, descripcionBandera, arribo, latitud, longitud,
                latitudParada, longitudParada, esAdaptado, identificadorCoche, ultimaFechaHoraGps,
                codigoLineaParada, distancia, minutos, null, null, null, null, null);
    }
}
