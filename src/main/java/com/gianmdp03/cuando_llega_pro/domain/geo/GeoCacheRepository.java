package com.gianmdp03.cuando_llega_pro.domain.geo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeoCacheRepository extends JpaRepository<GeoCacheEntity, String> {
}
