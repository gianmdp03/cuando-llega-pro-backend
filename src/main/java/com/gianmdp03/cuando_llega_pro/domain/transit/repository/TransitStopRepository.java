package com.gianmdp03.cuando_llega_pro.domain.transit.repository;

import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransitStopRepository extends JpaRepository<TransitStop, String> {

    @Query("""
            select distinct stop
            from TransitStop stop
            join stop.directions stopTransitLine
            where stopTransitLine.line.code = :codeTransitLine
              and stopTransitLine.direction = :direction
            order by stop.description asc
            """)
    List<TransitStop> findByCodigoTransitLineAndBandera(
            @Param("codeTransitLine") String codeTransitLine,
            @Param("direction") String direction
    );

    @Query("""
            select distinct stop
            from TransitStop stop
            left join fetch stop.directions stopTransitLine
            left join fetch stopTransitLine.line
            where stop.identifier = :identifier
            """)
    Optional<TransitStop> findByIdentificadorWithTransitLines(@Param("identifier") String identifier);
}
