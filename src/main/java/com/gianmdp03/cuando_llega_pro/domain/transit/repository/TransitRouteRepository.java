package com.gianmdp03.cuando_llega_pro.domain.transit.repository;

import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransitRouteRepository extends JpaRepository<TransitRoute, String> {

    @Query("""
            select distinct route
            from TransitRoute route
            left join fetch route.points
            where route.line.code = :lineCode
            order by route.branch asc
            """)
    List<TransitRoute> findByLineCodeWithPoints(@Param("lineCode") String lineCode);
}
