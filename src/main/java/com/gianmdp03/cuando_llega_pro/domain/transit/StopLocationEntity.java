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

import java.time.Instant;

/**
 * Persisted geographic location of a transit stop learned passively from live telemetry.
 */
@Entity
@Table(name = "stop_locations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
public class StopLocationEntity {

    @Id
    @Column(name = "stop_id", nullable = false, length = 100)
    private String stopId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
