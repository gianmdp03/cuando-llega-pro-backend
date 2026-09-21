package com.gianmdp03.cuando_llega_pro.domain.transit.repository;

import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransitLineRepository extends JpaRepository<TransitLine, String> {

    List<TransitLine> findAllByOrderByNameAsc();
}
