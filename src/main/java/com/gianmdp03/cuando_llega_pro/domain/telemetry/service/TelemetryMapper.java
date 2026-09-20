package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpArriboRaw;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpResponse;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service component responsible for parsing raw MGP telemetry payloads, normalizing arrival strings,
 * mapping to clean {@link ArrivalDTO} and {@link BusArrivalItemDTO} instances, and performing branch filtering.
 */
@Component
public class TelemetryMapper {

    private static final Logger log = LoggerFactory.getLogger(TelemetryMapper.class);

    private static final Pattern DIGIT_PATTERN = Pattern.compile("(\\d+)");
    private static final DateTimeFormatter GPS_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ObjectMapper objectMapper;

    public TelemetryMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public TelemetryMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Parses estimated remaining minutes from upstream raw arrival string (e.g., "53 min. aprox.", "arribando").
     * <ol>
     *   <li>Returns {@code 0} if string contains "arribando" or "proximo" (case-insensitive).</li>
     *   <li>Extracts leading digits via regex {@code Pattern.compile("(\\d+)")} (e.g. "53 min. aprox." -> 53).</li>
     *   <li>Returns {@code null} if no digits match or input is blank/null.</li>
     * </ol>
     *
     * @param arribo raw arrival string from upstream feed
     * @return remaining minutes or {@code null} if unparseable
     */
    public Integer parseRemainingMinutes(String arribo) {
        if (arribo == null || arribo.isBlank()) {
            return null;
        }

        String lower = arribo.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("arribando") || lower.contains("proximo") || lower.contains("próximo")) {
            return 0;
        }

        Matcher matcher = DIGIT_PATTERN.matcher(arribo.trim());
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Parses the upstream GPS telemetry timestamp ("dd/MM/yyyy HH:mm:ss").
     * Defaults to {@link Instant#now()} if timestamp is null, empty, or unparseable.
     *
     * @param ultimaFechaHoraGps date-time string in "dd/MM/yyyy HH:mm:ss" format
     * @return parsed Instant or Instant.now()
     */
    public Instant parseGpsTimestamp(String ultimaFechaHoraGps) {
        if (ultimaFechaHoraGps == null || ultimaFechaHoraGps.isBlank()) {
            return Instant.now();
        }

        String trimmed = ultimaFechaHoraGps.trim();
        try {
            LocalDateTime ldt = LocalDateTime.parse(trimmed, GPS_DATE_FORMATTER);
            return ldt.atZone(ARGENTINA_ZONE).toInstant();
        } catch (DateTimeParseException ex) {
            try {
                return Instant.parse(trimmed);
            } catch (Exception ignored) {
                log.debug("Failed to parse GPS timestamp: '{}', defaulting to Instant.now()", ultimaFechaHoraGps);
                return Instant.now();
            }
        }
    }

    /**
     * Maps an upstream raw telemetry item to a normalized {@link ArrivalDTO}.
     *
     * @param raw upstream raw arrival DTO
     * @return normalized ArrivalDTO
     */
    public ArrivalDTO toArrivalDTO(MgpArriboRaw raw) {
        return toArrivalDTO(raw, null);
    }

    /**
     * Maps an upstream raw telemetry item to a normalized {@link ArrivalDTO}, with optional fallback line code.
     *
     * @param raw              upstream raw arrival DTO
     * @param fallbackLineCode fallback line identifier if missing in raw DTO
     * @return normalized ArrivalDTO
     */
    public ArrivalDTO toArrivalDTO(MgpArriboRaw raw, String fallbackLineCode) {
        if (raw == null) {
            return null;
        }

        String lineCode = (raw.codigoLineaParada() != null && !raw.codigoLineaParada().isBlank())
                ? raw.codigoLineaParada().trim()
                : (raw.descripcionLinea() != null && !raw.descripcionLinea().isBlank()
                        ? raw.descripcionLinea().trim()
                        : fallbackLineCode);

        String branch = raw.descripcionBandera();
        String vehicleUnit = raw.identificadorCoche();
        Boolean accessible = "true".equalsIgnoreCase(raw.esAdaptado());

        Integer remainingMinutes = parseRemainingMinutes(raw.arribo());
        // For backwards compatibility with mocks supplying explicit minutes and clock time
        if (remainingMinutes == null && raw.minutos() != null) {
            remainingMinutes = raw.minutos();
        } else if (raw.arribo() != null && raw.arribo().contains(":") && raw.minutos() != null) {
            remainingMinutes = raw.minutos();
        }

        Instant timestamp = parseGpsTimestamp(raw.ultimaFechaHoraGps());

        return new ArrivalDTO(
                lineCode,
                branch,
                vehicleUnit,
                accessible,
                remainingMinutes,
                timestamp
        );
    }

    /**
     * Safely parses a coordinate string (e.g. "-38.000" or "-38,000") to Double.
     * Returns null if input is null, empty, or unparseable.
     *
     * @param coordinate raw coordinate string
     * @return parsed Double or null
     */
    public Double parseCoordinate(String coordinate) {
        if (coordinate == null || coordinate.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(coordinate.trim().replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Maps an upstream raw telemetry item directly to a {@link BusArrivalItemDTO}.
     *
     * @param raw              upstream raw arrival DTO
     * @param status           telemetry status (e.g. LIVE)
     * @param fallbackLineCode line code fallback
     * @return normalized BusArrivalItemDTO
     */
    public BusArrivalItemDTO toBusArrivalItemDTO(MgpArriboRaw raw, TelemetryStatus status, String fallbackLineCode) {
        if (raw == null) {
            return null;
        }

        ArrivalDTO arrival = toArrivalDTO(raw, fallbackLineCode);

        Integer distanceMeters = raw.distancia();
        if (distanceMeters == null && raw.latitud() != null && raw.longitud() != null
                && raw.latitudParada() != null && raw.longitudParada() != null) {
            distanceMeters = calculateHaversineDistanceMeters(
                    raw.latitud(), raw.longitud(),
                    raw.latitudParada(), raw.longitudParada()
            );
        }

        String estimatedArrival = raw.arribo() != null && !raw.arribo().isBlank()
                ? raw.arribo()
                : (arrival.remainingMinutes() != null ? arrival.remainingMinutes() + " min" : null);

        Double latitude = parseCoordinate(raw.latitud());
        Double longitude = parseCoordinate(raw.longitud());

        return new BusArrivalItemDTO(
                arrival.lineCode(),
                arrival.branch(),
                arrival.remainingMinutes(),
                distanceMeters,
                estimatedArrival,
                arrival.vehicleUnit(),
                arrival.accessible(),
                status,
                arrival.timestamp(),
                latitude,
                longitude
        );
    }

    /**
     * Overload mapping an upstream raw telemetry item to a {@link BusArrivalItemDTO} without fallbackLineCode.
     *
     * @param raw    upstream raw arrival DTO
     * @param status telemetry status
     * @return normalized BusArrivalItemDTO
     */
    public BusArrivalItemDTO toBusArrivalItemDTO(MgpArriboRaw raw, TelemetryStatus status) {
        return toBusArrivalItemDTO(raw, status, null);
    }

    /**
     * Maps parsed arrival items and raw telemetry to an ArrivalResponseDTO including parsed stop coordinates.
     *
     * @param lineCode      transit line code
     * @param stopId        transit stop ID
     * @param branch        route branch or variant
     * @param status        telemetry freshness status
     * @param timestamp     telemetry timestamp
     * @param deltaMinutes  elapsed minutes since last live update
     * @param arrivals      mapped bus arrival items
     * @param rawStopLat    raw stop latitude string
     * @param rawStopLon    raw stop longitude string
     * @return populated ArrivalResponseDTO
     */
    public ArrivalResponseDTO toArrivalResponseDTO(
            String lineCode,
            String stopId,
            String branch,
            TelemetryStatus status,
            Instant timestamp,
            Long deltaMinutes,
            List<BusArrivalItemDTO> arrivals,
            String rawStopLat,
            String rawStopLon
    ) {
        return new ArrivalResponseDTO(
                lineCode,
                stopId,
                branch,
                status,
                timestamp,
                deltaMinutes,
                arrivals,
                parseCoordinate(rawStopLat),
                parseCoordinate(rawStopLon)
        );
    }

    /**
     * Overload mapping an ArrivalResponseDTO using an MgpArriboRaw item to extract stop coordinates.
     */
    public ArrivalResponseDTO toArrivalResponseDTO(
            String lineCode,
            String stopId,
            String branch,
            TelemetryStatus status,
            Instant timestamp,
            Long deltaMinutes,
            List<BusArrivalItemDTO> arrivals,
            MgpArriboRaw raw
    ) {
        return toArrivalResponseDTO(
                lineCode,
                stopId,
                branch,
                status,
                timestamp,
                deltaMinutes,
                arrivals,
                raw != null ? raw.latitudParada() : null,
                raw != null ? raw.longitudParada() : null
        );
    }

    /**
     * Evaluates whether an arrival's branch matches the target preset branch case-insensitively.
     * If the target preset branch is null or empty, all branches match.
     *
     * @param arrivalBranch route branch / variant from the arrival item
     * @param targetBandera target branch from user preset configuration
     * @return true if the arrival matches the target branch criteria
     */
    public boolean matchesBranch(String arrivalBranch, String targetBandera) {
        if (targetBandera == null || targetBandera.isBlank()) {
            return true;
        }
        if (arrivalBranch == null) {
            return false;
        }
        return arrivalBranch.trim().equalsIgnoreCase(targetBandera.trim());
    }

    /**
     * Filters a list of {@link ArrivalDTO} items by preset branch case-insensitively.
     * If the target branch is null or empty, all items are included.
     *
     * @param arrivals      list of arrival DTOs
     * @param targetBandera route branch / variant criteria
     * @return filtered list of ArrivalDTOs
     */
    public List<ArrivalDTO> filterByBranch(List<ArrivalDTO> arrivals, String targetBandera) {
        if (arrivals == null || arrivals.isEmpty()) {
            return List.of();
        }
        if (targetBandera == null || targetBandera.isBlank()) {
            return arrivals.stream().filter(Objects::nonNull).toList();
        }
        return arrivals.stream()
                .filter(Objects::nonNull)
                .filter(item -> matchesBranch(item.branch(), targetBandera))
                .toList();
    }

    /**
     * Filters a list of {@link BusArrivalItemDTO} items by preset branch case-insensitively.
     * If the target branch is null or empty, all items are included.
     *
     * @param arrivals      list of bus arrival item DTOs
     * @param targetBandera route branch / variant criteria
     * @return filtered list of BusArrivalItemDTOs
     */
    public List<BusArrivalItemDTO> filterBusArrivalsByBranch(List<BusArrivalItemDTO> arrivals, String targetBandera) {
        if (arrivals == null || arrivals.isEmpty()) {
            return List.of();
        }
        if (targetBandera == null || targetBandera.isBlank()) {
            return arrivals.stream().filter(Objects::nonNull).toList();
        }
        return arrivals.stream()
                .filter(Objects::nonNull)
                .filter(item -> matchesBranch(item.branch(), targetBandera))
                .toList();
    }

    /**
     * Deserializes raw upstream JSON into a list of {@link MgpArriboRaw} items.
     * Supports wrapped objects ({@link MgpResponse}) as well as bare JSON arrays.
     *
     * @param rawJson upstream JSON payload
     * @return list of parsed MgpArriboRaw items
     * @throws JsonProcessingException if JSON is malformed
     */
    public List<MgpArriboRaw> parseRawArrivals(String rawJson) throws JsonProcessingException {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        MgpResponse response = objectMapper.readValue(rawJson, MgpResponse.class);
        return response.arribos() != null ? response.arribos() : List.of();
    }

    private Integer calculateHaversineDistanceMeters(String lat1Str, String lon1Str, String lat2Str, String lon2Str) {
        try {
            double lat1 = Double.parseDouble(lat1Str.trim().replace(',', '.'));
            double lon1 = Double.parseDouble(lon1Str.trim().replace(',', '.'));
            double lat2 = Double.parseDouble(lat2Str.trim().replace(',', '.'));
            double lon2 = Double.parseDouble(lon2Str.trim().replace(',', '.'));

            final int R = 6371000; // Earth radius in meters
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return (int) Math.round(R * c);
        } catch (Exception e) {
            return null;
        }
    }
}
