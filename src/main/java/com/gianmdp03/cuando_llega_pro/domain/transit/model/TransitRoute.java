package com.gianmdp03.cuando_llega_pro.domain.transit.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Ordered road geometry published by MGP for one line branch. */
@Entity
@Table(name = "transit_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransitRoute {

    @Id
    @Column(length = 500)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "line_code", nullable = false)
    private TransitLine line;

    @Column(nullable = false, length = 500)
    private String branch;

    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransitRoutePoint> points = new ArrayList<>();

    public TransitRoute(String id, TransitLine line, String branch, String description) {
        this.id = id;
        this.line = line;
        this.branch = branch;
        this.description = description;
    }

    public void updateMetadata(TransitLine line, String branch, String description) {
        this.line = line;
        this.branch = branch;
        this.description = description;
    }

    public void replacePoints(List<TransitRoutePoint> replacement) {
        points.clear();
        replacement.forEach(this::addPoint);
    }

    private void addPoint(TransitRoutePoint point) {
        points.add(point);
        point.setRoute(this);
    }
}
