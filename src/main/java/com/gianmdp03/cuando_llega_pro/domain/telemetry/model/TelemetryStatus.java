package com.gianmdp03.cuando_llega_pro.domain.telemetry.model;

/**
 * Represents the health, currency, and calculation state of transit arrival telemetry.
 */
public enum TelemetryStatus {
    /**
     * Direct live telemetry received within real-time tolerance.
     */
    LIVE,

    /**
     * Extrapolated arrival time derived from cached telemetry (1 to 25 minutes elapsed).
     */
    ESTIMATED_FALLBACK,

    /**
     * Expired telemetry where real-time signal has been lost for more than 25 minutes.
     */
    EXPIRED,

    /**
     * The upstream could not provide telemetry and no usable fallback existed.
     */
    UNAVAILABLE
}
