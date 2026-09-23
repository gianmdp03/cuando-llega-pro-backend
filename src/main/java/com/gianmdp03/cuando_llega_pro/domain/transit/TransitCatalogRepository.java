package com.gianmdp03.cuando_llega_pro.domain.transit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for persisting catalog entries in PostgreSQL.
 */
@Repository
public interface TransitCatalogRepository extends JpaRepository<TransitCatalogEntity, String> {

    List<TransitCatalogEntity> findAllByCatalogType(String catalogType);

    @Modifying
    @Transactional
    @Query("DELETE FROM TransitCatalogEntity t WHERE t.updatedAt < :cutoff AND t.catalogType != 'SOURCE_METADATA'")
    int deleteStaleEntries(@Param("cutoff") Instant cutoff);
}
