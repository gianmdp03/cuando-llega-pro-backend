package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.MapLineDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.LineDirectionDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopDetailDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.MapStopDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.DirectionDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitLineRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.StopLineDirectionRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Query service for the persisted, static transport map catalogue. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransitService {

    private final TransitLineRepository lineRepository;
    private final TransitStopRepository stopRepository;
    private final StopLineDirectionRepository stopLineDirectionRepository;

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'lines'")
    public List<MapLineDto> getLines() {
        return lineRepository.findAllByOrderByNameAsc().stream()
                .map(line -> new MapLineDto(line.getCode(), line.getName()))
                .toList();
    }

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'directions:' + #codeTransitLine")
    public List<DirectionDto> getDirections(String codeTransitLine) {
        requireLine(codeTransitLine);
        Map<String, DirectionDto> directions = new LinkedHashMap<>();
        stopLineDirectionRepository.findDistinctDirectionDtosByLineCode(codeTransitLine)
                .forEach(direction -> directions.putIfAbsent(direction.direction(), direction));
        return List.copyOf(directions.values());
    }

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'stops:' + #codeTransitLine + ':' + #direction")
    public List<MapStopDto> getStops(String codeTransitLine, String direction) {
        requireLine(codeTransitLine);
        return stopRepository.findByLineCodeAndDirection(codeTransitLine, direction).stream()
                .map(this::toMapStop)
                .toList();
    }

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'stop:' + #identifier")
    public StopDetailDto getStop(String identifier) {
        TransitStop stop = stopRepository.findByIdentifierWithDirections(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Stop not found: " + identifier));
        List<LineDirectionDto> directions = stop.getDirections().stream()
                .sorted((left, right) -> {
                    int byName = left.getLine().getName().compareTo(right.getLine().getName());
                    return byName != 0 ? byName : left.getDirection().compareTo(right.getDirection());
                })
                .map(this::toLineDirection)
                .toList();
        return new StopDetailDto(stop.getIdentifier(), stop.getLatitude(), stop.getLongitude(), directions);
    }

    private void requireLine(String codeTransitLine) {
        if (!lineRepository.existsById(codeTransitLine)) {
            throw new ResourceNotFoundException("Line not found: " + codeTransitLine);
        }
    }

    private MapStopDto toMapStop(TransitStop stop) {
        return new MapStopDto(
                stop.getIdentifier(),
                stop.getCode(),
                stop.getDescription(),
                stop.getLatitude(),
                stop.getLongitude()
        );
    }

    private LineDirectionDto toLineDirection(StopLineDirection stopLineDirection) {
        return new LineDirectionDto(
                stopLineDirection.getLine().getCode(),
                stopLineDirection.getLine().getName(),
                stopLineDirection.getDirection(),
                stopLineDirection.getExpandedDirection()
        );
    }
}
