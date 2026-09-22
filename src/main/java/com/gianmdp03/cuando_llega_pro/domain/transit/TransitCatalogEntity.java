package com.gianmdp03.cuando_llega_pro.domain.transit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * PostgreSQL persistence entity for caching municipal transit catalog data (lines, stops, routes).
 * Supports Cache-Aside L2 storage to avoid mass scraping of the upstream proxy.
 */
@Entity
@Table(name = "transit_catalog_entries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
public class TransitCatalogEntity {

    @Id
    @Column(name = "cache_key", nullable = false, length = 150)
    private String cacheKey;

    @Column(name = "catalog_type", nullable = false, length = 50)
    private String catalogType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

