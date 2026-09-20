package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;

import java.time.Instant;

/**
 * Normalized transit telemetry item representing an individual bus arrival.
 *
 * @param lineCode             transit line code (e.g. "511", "522")
 * @param branch               route branch or destination variant (bandera)
 * @param remainingMinutes     remaining minutes until arrival
 * @param distanceMeters       distance in meters until arrival
 * @param estimatedArrivalTime formatted string representation of arrival time
 * @param vehicleUnit          transit bus unit number (coche)
 * @param accessible           indicates whether vehicle has wheelchair access (adaptado)
 * @param status               telemetry status of this arrival prediction
 * @param timestamp            optional telemetry recording timestamp
 * @param latitude             vehicle GPS latitude coordinate
 * @param longitude            vehicle GPS longitude coordinate
 */
public record BusArrivalItemDTO(
        String lineCode,
        String branch,
        Integer remainingMinutes,
        Integer distanceMeters,
        String estimatedArrivalTime,
        String vehicleUnit,
        Boolean accessible,
        TelemetryStatus status,
        Instant timestamp,
        Double latitude,
        Double longitude
) {

    public BusArrivalItemDTO(
            String lineCode,
            String branch,
            Integer remainingMinutes,
            Integer distanceMeters,
            String estimatedArrivalTime,
            String vehicleUnit,
            Boolean accessible,
            TelemetryStatus status,
            Instant timestamp
    ) {
        this(lineCode, branch, remainingMinutes, distanceMeters, estimatedArrivalTime, vehicleUnit, accessible, status, timestamp, null, null);
    }

    public BusArrivalItemDTO(
            String lineCode,
            String branch,
            Integer remainingMinutes,
            Integer distanceMeters,
            String estimatedArrivalTime,
            String vehicleUnit,
            Boolean accessible,
            TelemetryStatus status
    ) {
        this(lineCode, branch, remainingMinutes, distanceMeters, estimatedArrivalTime, vehicleUnit, accessible, status, null, null, null);
    }

    public BusArrivalItemDTO(String busId, String line, String destination, long remainingMinutes, Integer distanceMeters, TelemetryStatus status, Instant timestamp) {
        this(line, destination, (int) Math.max(0L, remainingMinutes), distanceMeters, null, busId, null, status, timestamp, null, null);
    }

    public BusArrivalItemDTO(String busId, String line, String destination, long remainingMinutes, TelemetryStatus status) {
        this(line, destination, (int) Math.max(0L, remainingMinutes), null, null, busId, null, status, null, null, null);
    }

    public BusArrivalItemDTO(String busId, String line, long remainingMinutes, TelemetryStatus status) {
        this(line, null, (int) Math.max(0L, remainingMinutes), null, null, busId, null, status, null, null, null);
    }

    public BusArrivalItemDTO(String busId, long remainingMinutes, TelemetryStatus status) {
        this(null, null, (int) Math.max(0L, remainingMinutes), null, null, busId, null, status, null, null, null);
    }

    /**
     * Alias for {@link #vehicleUnit()} or line code if vehicleUnit is absent.
     */
    public String busId() {
        return vehicleUnit != null ? vehicleUnit : lineCode;
    }

    /**
     * Alias for {@link #lineCode()}.
     */
    public String line() {
        return lineCode;
    }

    /**
     * Alias for {@link #branch()}.
     */
    public String destination() {
        return branch;
    }

    /**
     * Creates a new instance preserving original attributes while updating status and remaining minutes.
     *
     * @param newStatus           the updated telemetry status
     * @param newRemainingMinutes the updated remaining minutes
     * @return a new BusArrivalItemDTO with updated status and remaining minutes
     */
    public BusArrivalItemDTO withStatusAndRemainingMinutes(TelemetryStatus newStatus, long newRemainingMinutes) {
        return new BusArrivalItemDTO(
                this.lineCode,
                this.branch,
                (int) Math.max(0L, newRemainingMinutes),
                this.distanceMeters,
                this.estimatedArrivalTime,
                this.vehicleUnit,
                this.accessible,
                newStatus,
                this.timestamp,
                this.latitude,
                this.longitude
        );
    }

    /**
     * Converts this BusArrivalItemDTO to an ArrivalDTO.
     *
     * @return equivalent ArrivalDTO
     */
    public ArrivalDTO toArrivalDTO() {
        return new ArrivalDTO(
                this.lineCode,
                this.branch,
                this.vehicleUnit,
                this.accessible,
                this.remainingMinutes,
                this.timestamp
        );
    }

    /**
     * Factory method creating a BusArrivalItemDTO from an ArrivalDTO.
     *
     * @param dto    ArrivalDTO instance
     * @param status telemetry status
     * @return equivalent BusArrivalItemDTO
     */
    public static BusArrivalItemDTO fromArrivalDTO(ArrivalDTO dto, TelemetryStatus status) {
        if (dto == null) {
            return null;
        }
        return dto.toBusArrivalItemDTO(status);
    }
}
