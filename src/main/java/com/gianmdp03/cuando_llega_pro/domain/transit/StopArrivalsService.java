package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.ArrivalsService;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.TelemetryMapper;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopArrivalsDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopDirectionArrivalsDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopLineArrivalsDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Combines static stop catalogue data with live telemetry without exposing upstream fan-out to map clients. */
@Service
@RequiredArgsConstructor
public class StopArrivalsService {

    private final TransitStopRepository stopRepository;
    private final ArrivalsService arrivalsService;
    private final TelemetryMapper telemetryMapper;

    public StopArrivalsDto getArrivals(String identifier) {
        TransitStop stop = loadStop(identifier);

        List<StopLineArrivalsDto> lines = groupDirectionsByLine(stop).entrySet().stream()
                .map(entry -> getLineArrivals(stop.getIdentifier(), entry.getValue()))
                .toList();

        return new StopArrivalsDto(stop.getIdentifier(), stop.getLatitude(), stop.getLongitude(), lines);
    }

    /** Validates that a stop exists before an HTTP response is committed as an event stream. */
    public void requireStop(String identifier) {
        loadStop(identifier);
    }

    private Map<String, List<StopLineDirection>> groupDirectionsByLine(TransitStop stop) {
        return stop.getDirections().stream()
                .sorted(Comparator.comparing(direction -> direction.getLine().getName()))
                .collect(java.util.stream.Collectors.groupingBy(
                        direction -> direction.getLine().getCode(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
    }

    private TransitStop loadStop(String identifier) {
        return stopRepository.findByIdentifierWithDirections(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found: " + identifier));
    }

    private StopLineArrivalsDto getLineArrivals(String stopIdentifier, List<StopLineDirection> directions) {
        StopLineDirection firstDirection = directions.getFirst();
        String commercialLineCode = firstDirection.getLine().getName();
        List<StopDirectionArrivalsDto> directionDtos = directions.stream()
                .sorted(Comparator.comparing(StopLineDirection::getDirection))
                .map(direction -> new StopDirectionArrivalsDto(
                        direction.getDirection(),
                        direction.getExpandedDirection(),
                        List.of()
                ))
                .toList();

        try {
            ArrivalResponseDTO telemetry = arrivalsService.getArrivals(commercialLineCode, stopIdentifier);
            List<StopDirectionArrivalsDto> populatedDirections = directions.stream()
                    .sorted(Comparator.comparing(StopLineDirection::getDirection))
                    .map(direction -> new StopDirectionArrivalsDto(
                            direction.getDirection(),
                            direction.getExpandedDirection(),
                            telemetryMapper.filterBusArrivalsByBranch(telemetry.arrivals(), direction.getDirection())
                    ))
                    .toList();
            return new StopLineArrivalsDto(
                    commercialLineCode,
                    telemetry.status().name(),
                    telemetry.timestamp(),
                    telemetry.deltaMinutes(),
                    populatedDirections,
                    null
            );
        } catch (UpstreamServiceException exception) {
            return new StopLineArrivalsDto(
                    commercialLineCode,
                    "UNAVAILABLE",
                    null,
                    null,
                    directionDtos,
                    exception.getMessage()
            );
        }
    }
}
