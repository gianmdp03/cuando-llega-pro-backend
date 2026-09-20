package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;

import java.time.Instant;

/**
 * Data transfer object representing an individual arrival item.
 * Compatible and interchangeable with {@link BusArrivalItemDTO}.
 *
 * @param busId            unique bus identifier or fleet number
 * @param line             bus line name or code
 * @param destination      route destination description
 * @param remainingMinutes estimated minutes until arrival
 * @param distanceMeters   estimated distance in meters to the stop
 * @param status           telemetry status of the arrival item
 * @param timestamp        telemetry recording timestamp
 */
public record ArrivalItemDTO(
        String busId,
        String line,
        String destination,
        long remainingMinutes,
        Integer distanceMeters,
        TelemetryStatus status,
        Instant timestamp
) {

    public ArrivalItemDTO(String busId, String line, long remainingMinutes, TelemetryStatus status) {
        this(busId, line, null, remainingMinutes, null, status, null);
    }

    public ArrivalItemDTO(String busId, long remainingMinutes, TelemetryStatus status) {
        this(busId, null, null, remainingMinutes, null, status, null);
    }

    public BusArrivalItemDTO toBusArrivalItemDTO() {
        return new BusArrivalItemDTO(
                this.busId,
                this.line,
                this.destination,
                this.remainingMinutes,
                this.distanceMeters,
                this.status,
                this.timestamp
        );
    }

    public static ArrivalItemDTO from(BusArrivalItemDTO item) {
        if (item == null) {
            return null;
        }
        return new ArrivalItemDTO(
                item.busId(),
                item.line(),
                item.destination(),
                item.remainingMinutes(),
                item.distanceMeters(),
                item.status(),
                item.timestamp()
        );
    }
}
