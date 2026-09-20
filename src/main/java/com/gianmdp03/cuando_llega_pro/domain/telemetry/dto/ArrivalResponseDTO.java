package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Normalized transit telemetry response for a given bus stop and line code.
 *
 * @param lineCode     queried transit line code
 * @param stopId       queried bus stop identifier
 * @param branch       predominant branch or route identifier
 * @param status       telemetry freshness status (LIVE, ESTIMATED_FALLBACK, or EXPIRED)
 * @param timestamp    timestamp when telemetry was recorded or extrapolated
 * @param deltaMinutes minutes elapsed since the last live telemetry update
 * @param arrivals     list of upcoming bus arrivals
 * @param stopLatitude bus stop GPS latitude coordinate
 * @param stopLongitude bus stop GPS longitude coordinate
 */
public record ArrivalResponseDTO(
        String lineCode,
        String stopId,
        String branch,
        TelemetryStatus status,
        Instant timestamp,
        Long deltaMinutes,
        List<BusArrivalItemDTO> arrivals,
        Double stopLatitude,
        Double stopLongitude
) {

    public ArrivalResponseDTO {
        arrivals = (arrivals != null)
                ? arrivals.stream().filter(Objects::nonNull).toList()
                : List.of();
    }

    public ArrivalResponseDTO(
            String lineCode,
            String stopId,
            String branch,
            TelemetryStatus status,
            Instant timestamp,
            Long deltaMinutes,
            List<BusArrivalItemDTO> arrivals
    ) {
        this(lineCode, stopId, branch, status, timestamp, deltaMinutes, arrivals, null, null);
    }

    public ArrivalResponseDTO(
            String stopId,
            String lineCode,
            TelemetryStatus status,
            Instant timestamp,
            long deltaMinutes,
            List<BusArrivalItemDTO> arrivals
    ) {
        this(lineCode, stopId, null, status, timestamp, deltaMinutes, arrivals, null, null);
    }

    public ArrivalResponseDTO(
            String stopId,
            String lineCode,
            TelemetryStatus status,
            Instant timestamp,
            List<BusArrivalItemDTO> arrivals
    ) {
        this(lineCode, stopId, null, status, timestamp, 0L, arrivals, null, null);
    }

    public ArrivalResponseDTO(
            TelemetryStatus status,
            Instant timestamp,
            long deltaMinutes,
            List<BusArrivalItemDTO> arrivals
    ) {
        this(null, null, null, status, timestamp, deltaMinutes, arrivals, null, null);
    }

    public ArrivalResponseDTO(
            TelemetryStatus status,
            Instant timestamp,
            List<BusArrivalItemDTO> arrivals
    ) {
        this(null, null, null, status, timestamp, 0L, arrivals, null, null);
    }

    /**
     * Terminology alias for {@link #arrivals()}.
     *
     * @return unmodifiable list of bus arrival items
     */
    public List<BusArrivalItemDTO> items() {
        return arrivals();
    }

    /**
     * Factory method creating an empty arrival response.
     *
     * @param lineCode transit line code
     * @param stopId   bus stop identifier
     * @param status   telemetry status
     * @return empty ArrivalResponseDTO
     */
    public static ArrivalResponseDTO empty(String lineCode, String stopId, TelemetryStatus status) {
        return new ArrivalResponseDTO(
                lineCode,
                stopId,
                null,
                status,
                Instant.now(),
                0L,
                List.of()
        );
    }
}
