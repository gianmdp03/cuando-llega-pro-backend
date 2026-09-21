package com.gianmdp03.cuando_llega_pro.domain.transit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for persisting catalog entries in PostgreSQL.
 */
@Repository
public interface TransitCatalogRepository extends JpaRepository<TransitCatalogEntity, String> {

    List<TransitCatalogEntity> findAllByCatalogType(String catalogType);
}
