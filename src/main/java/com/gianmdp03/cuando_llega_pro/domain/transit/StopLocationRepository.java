package com.gianmdp03.cuando_llega_pro.domain.transit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for persisted transit stop locations.
 */
@Repository
public interface StopLocationRepository extends JpaRepository<StopLocationEntity, String> {
}
