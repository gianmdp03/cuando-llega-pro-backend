package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.MgpArriboRaw;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service façade for MGP telemetry parsing and mapping operations.
 * Delegates mapping and parsing logic to {@link TelemetryMapper}.
 */
@Service
public class MgpService {

    private final TelemetryMapper telemetryMapper;

    public MgpService(TelemetryMapper telemetryMapper) {
        this.telemetryMapper = telemetryMapper != null ? telemetryMapper : new TelemetryMapper();
    }

    public Integer parseRemainingMinutes(String arribo) {
        return telemetryMapper.parseRemainingMinutes(arribo);
    }

    public Instant parseGpsTimestamp(String ultimaFechaHoraGps) {
        return telemetryMapper.parseGpsTimestamp(ultimaFechaHoraGps);
    }

    public ArrivalDTO toArrivalDTO(MgpArriboRaw raw) {
        return telemetryMapper.toArrivalDTO(raw);
    }

    public ArrivalDTO toArrivalDTO(MgpArriboRaw raw, String fallbackLineCode) {
        return telemetryMapper.toArrivalDTO(raw, fallbackLineCode);
    }

    public BusArrivalItemDTO toBusArrivalItemDTO(MgpArriboRaw raw, TelemetryStatus status, String fallbackLineCode) {
        return telemetryMapper.toBusArrivalItemDTO(raw, status, fallbackLineCode);
    }

    public boolean matchesBranch(String arrivalBranch, String targetBandera) {
        return telemetryMapper.matchesBranch(arrivalBranch, targetBandera);
    }

    public List<ArrivalDTO> filterByBranch(List<ArrivalDTO> arrivals, String targetBandera) {
        return telemetryMapper.filterByBranch(arrivals, targetBandera);
    }

    public List<BusArrivalItemDTO> filterBusArrivalsByBranch(List<BusArrivalItemDTO> arrivals, String targetBandera) {
        return telemetryMapper.filterBusArrivalsByBranch(arrivals, targetBandera);
    }

    public List<MgpArriboRaw> parseRawArrivals(String rawJson) throws JsonProcessingException {
        return telemetryMapper.parseRawArrivals(rawJson);
    }
}
