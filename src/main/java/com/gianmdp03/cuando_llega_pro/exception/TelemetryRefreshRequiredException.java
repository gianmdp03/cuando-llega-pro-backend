package com.gianmdp03.cuando_llega_pro.exception;

public class TelemetryRefreshRequiredException extends RuntimeException {
    public TelemetryRefreshRequiredException(String lineCode, String stopId) {
        super("Se requiere renovar arribos desde un cliente MGP para línea " + lineCode + ", parada " + stopId);
    }
}
