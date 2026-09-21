package com.gianmdp03.cuando_llega_pro.domain.transit.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/** A physical stop, which can serve several lines and directions. */
@Entity
@Table(name = "stops")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransitStop {

    @Id
    @Column(name = "identifier", nullable = false, length = 100)
    private String identifier;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @OneToMany(mappedBy = "stop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<StopLineDirection> directions = new LinkedHashSet<>();

    public TransitStop(String identifier, String code, String description, Double latitude, Double longitude) {
        this.identifier = identifier;
        this.code = code;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void addTransitLine(StopLineDirection stopTransitLine) {
        directions.add(stopTransitLine);
        stopTransitLine.setStop(this);
    }

    public void removeTransitLine(StopLineDirection stopTransitLine) {
        directions.remove(stopTransitLine);
        stopTransitLine.setStop(null);
    }
}
