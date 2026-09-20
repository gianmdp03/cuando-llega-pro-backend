package com.gianmdp03.cuando_llega_pro.domain.telemetry.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;

import java.time.Instant;

/**
 * Clean data transfer object representing a parsed and normalized bus arrival.
 *
 * @param lineCode         transit line code (e.g. "511", "522")
 * @param branch           route branch / variant (bandera)
 * @param vehicleUnit      transit vehicle identifier / fleet number (coche)
 * @param accessible       whether the vehicle has accessibility / wheelchair ramp
 * @param remainingMinutes estimated remaining minutes until arrival at stop
 * @param timestamp        recording timestamp of the GPS telemetry point
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArrivalDTO(
        @JsonProperty("lineCode") String lineCode,
        @JsonProperty("branch") String branch,
        @JsonProperty("vehicleUnit") String vehicleUnit,
        @JsonProperty("accessible") Boolean accessible,
        @JsonProperty("remainingMinutes") Integer remainingMinutes,
        @JsonProperty("timestamp") Instant timestamp
) {

    /**
     * Converts this ArrivalDTO to a BusArrivalItemDTO for dashboard and service compatibility.
     *
     * @param status telemetry freshness status
     * @return equivalent BusArrivalItemDTO
     */
    public BusArrivalItemDTO toBusArrivalItemDTO(TelemetryStatus status) {
        return new BusArrivalItemDTO(
                lineCode,
                branch,
                remainingMinutes,
                null,
                remainingMinutes != null ? remainingMinutes + " min" : null,
                vehicleUnit,
                accessible,
                status,
                timestamp
        );
    }

    /**
     * Factory method to create an ArrivalDTO from a BusArrivalItemDTO.
     *
     * @param item BusArrivalItemDTO instance
     * @return equivalent ArrivalDTO
     */
    public static ArrivalDTO fromBusArrivalItem(BusArrivalItemDTO item) {
        if (item == null) {
            return null;
        }
        return new ArrivalDTO(
                item.lineCode(),
                item.branch(),
                item.vehicleUnit(),
                item.accessible(),
                item.remainingMinutes(),
                item.timestamp()
        );
    }
}
