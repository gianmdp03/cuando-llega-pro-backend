package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Service providing the predictive fallback arrival extrapolation algorithm.
 * Extrapolates bus arrival predictions when live updates are delayed or unavailable,
 * maintaining high-reliability transit ETAs based on elapsed cache time.
 */
@Service
public class ExtrapolationEngine {

    /**
     * Maximum duration in minutes for which cached telemetry is considered extrapolatable.
     * Beyond this threshold, telemetry is declared EXPIRED.
     */
    public static final long EXPIRATION_THRESHOLD_MINUTES = 25L;

    /**
     * Extrapolates arrival predictions using the current wall-clock time.
     *
     * @param cachedTelemetry cached telemetry response to extrapolate
     * @return extrapolated arrival response
     * @throws IllegalArgumentException if cachedTelemetry or its timestamp is null
     */
    public ArrivalResponseDTO extrapolate(ArrivalResponseDTO cachedTelemetry) {
        return extrapolate(cachedTelemetry, Instant.now());
    }

    /**
     * Implements the predictive fallback arrival extrapolation algorithm:
     * <ul>
     *   <li>Calculates delta time: &Delta;t = Duration.between(cachedTelemetry.timestamp(), currentTime).toMinutes()</li>
     *   <li>If &Delta;t &le; 0: status is LIVE, remaining minutes unchanged.</li>
     *   <li>If &Delta;t &gt; 25: status is EXPIRED, overall response and all items tagged EXPIRED, remaining minutes set to 0.</li>
     *   <li>If 0 &lt; &Delta;t &le; 25: status is ESTIMATED_FALLBACK, for each item remainingMinutes = max(0, item.remainingMinutes() - &Delta;t) and status is ESTIMATED_FALLBACK.</li>
     * </ul>
     *
     * @param cachedTelemetry cached telemetry snapshot
     * @param currentTime     current reference time
     * @return a new ArrivalResponseDTO with updated status, deltaMinutes, and extrapolated items
     * @throws IllegalArgumentException if cachedTelemetry, currentTime, or cached telemetry timestamp is null
     */
    public ArrivalResponseDTO extrapolate(ArrivalResponseDTO cachedTelemetry, Instant currentTime) {
        if (cachedTelemetry == null) {
            throw new IllegalArgumentException("cachedTelemetry must not be null");
        }
        if (currentTime == null) {
            throw new IllegalArgumentException("currentTime must not be null");
        }
        if (cachedTelemetry.timestamp() == null) {
            throw new IllegalArgumentException("cachedTelemetry timestamp must not be null");
        }

        long deltaMinutes = Duration.between(cachedTelemetry.timestamp(), currentTime).toMinutes();

        TelemetryStatus targetStatus;
        List<BusArrivalItemDTO> extrapolatedItems;

        List<BusArrivalItemDTO> sourceItems = (cachedTelemetry.arrivals() != null)
                ? cachedTelemetry.arrivals()
                : List.of();

        if (deltaMinutes <= 0) {
            targetStatus = TelemetryStatus.LIVE;
            extrapolatedItems = sourceItems.stream()
                    .filter(Objects::nonNull)
                    .map(item -> item.withStatusAndRemainingMinutes(
                            TelemetryStatus.LIVE,
                            item.remainingMinutes()
                    ))
                    .toList();
        } else if (deltaMinutes > EXPIRATION_THRESHOLD_MINUTES) {
            targetStatus = TelemetryStatus.EXPIRED;
            extrapolatedItems = sourceItems.stream()
                    .filter(Objects::nonNull)
                    .map(item -> item.withStatusAndRemainingMinutes(
                            TelemetryStatus.EXPIRED,
                            0L
                    ))
                    .toList();
        } else {
            targetStatus = TelemetryStatus.ESTIMATED_FALLBACK;
            extrapolatedItems = sourceItems.stream()
                    .filter(Objects::nonNull)
                    .map(item -> item.withStatusAndRemainingMinutes(
                            TelemetryStatus.ESTIMATED_FALLBACK,
                            Math.max(0L, item.remainingMinutes() - deltaMinutes)
                    ))
                    .toList();
        }

        return new ArrivalResponseDTO(
                cachedTelemetry.lineCode(),
                cachedTelemetry.stopId(),
                cachedTelemetry.branch(),
                targetStatus,
                cachedTelemetry.timestamp(),
                deltaMinutes,
                extrapolatedItems,
                cachedTelemetry.stopLatitude(),
                cachedTelemetry.stopLongitude()
        );
    }

    /**
     * Deterministically extrapolates an individual vehicle unit arrival prediction by IdentificadorCoche.
     *
     * @param item        the vehicle arrival item to extrapolate
     * @param currentTime the reference wall-clock time
     * @return extrapolated BusArrivalItemDTO with updated status and remainingMinutes
     */
    public BusArrivalItemDTO extrapolateVehicle(BusArrivalItemDTO item, Instant currentTime) {
        if (item == null) {
            return null;
        }
        Instant refTime = item.timestamp() != null ? item.timestamp() : currentTime;
        long delta = Duration.between(refTime, currentTime).toMinutes();

        if (delta <= 0) {
            return item.withStatusAndRemainingMinutes(
                    TelemetryStatus.LIVE,
                    item.remainingMinutes() != null ? item.remainingMinutes() : 0L
            );
        } else if (delta > EXPIRATION_THRESHOLD_MINUTES) {
            return item.withStatusAndRemainingMinutes(TelemetryStatus.EXPIRED, 0L);
        } else {
            long remaining = (item.remainingMinutes() != null)
                    ? Math.max(0L, item.remainingMinutes() - delta)
                    : 0L;
            return item.withStatusAndRemainingMinutes(TelemetryStatus.ESTIMATED_FALLBACK, remaining);
        }
    }

    /**
     * Alias for {@link #extrapolateVehicle(BusArrivalItemDTO, Instant)} using wall-clock time.
     */
    public BusArrivalItemDTO extrapolateVehicle(BusArrivalItemDTO item) {
        return extrapolateVehicle(item, Instant.now());
    }

    /**
     * Alias for {@link #extrapolate(ArrivalResponseDTO, Instant)}.
     *
     * @param cachedTelemetry cached telemetry snapshot
     * @param currentTime     current reference time
     * @return extrapolated arrival response
     */
    public ArrivalResponseDTO extrapolateArrivals(ArrivalResponseDTO cachedTelemetry, Instant currentTime) {
        return extrapolate(cachedTelemetry, currentTime);
    }

    /**
     * Alias for {@link #extrapolate(ArrivalResponseDTO)}.
     *
     * @param cachedTelemetry cached telemetry snapshot
     * @return extrapolated arrival response
     */
    public ArrivalResponseDTO extrapolateArrivals(ArrivalResponseDTO cachedTelemetry) {
        return extrapolate(cachedTelemetry);
    }
}
