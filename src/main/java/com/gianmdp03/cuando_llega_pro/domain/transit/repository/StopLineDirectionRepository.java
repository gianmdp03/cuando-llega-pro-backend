package com.gianmdp03.cuando_llega_pro.domain.transit.repository;

import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.DirectionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StopLineDirectionRepository extends JpaRepository<StopLineDirection, Long> {

    @Query("""
            select distinct stopTransitLine.direction
            from StopLineDirection stopTransitLine
            where stopTransitLine.line.code = :lineCode
            order by stopTransitLine.direction asc
            """)
    List<String> findDistinctDirectionsByLineCode(@Param("lineCode") String lineCode);

    @Query("""
            select distinct new com.gianmdp03.cuando_llega_pro.domain.transit.dto.DirectionDto(
                stopTransitLine.direction, stopTransitLine.expandedDirection
            )
            from StopLineDirection stopTransitLine
            where stopTransitLine.line.code = :lineCode
            order by stopTransitLine.direction asc, stopTransitLine.expandedDirection asc
            """)
    List<DirectionDto> findDistinctDirectionDtosByLineCode(@Param("lineCode") String lineCode);

    Optional<StopLineDirection> findByStopIdentifierAndLineCodeAndDirection(
            String identifier,
            String codeTransitLine,
            String direction
    );
}
