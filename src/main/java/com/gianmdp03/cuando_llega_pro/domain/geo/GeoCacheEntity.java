package com.gianmdp03.cuando_llega_pro.domain.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "geo_cache")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString
public class GeoCacheEntity {

    @Id
    @Column(name = "grid_key", length = 32, nullable = false)
    private String gridKey;

    @Column(name = "formatted_address", nullable = false, length = 300)
    private String formattedAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GeoCacheEntity(String gridKey, String formattedAddress) {
        this.gridKey = gridKey;
        this.formattedAddress = formattedAddress;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
