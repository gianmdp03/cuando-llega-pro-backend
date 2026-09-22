package com.gianmdp03.cuando_llega_pro.domain.transit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One ordered vertex of an official MGP route geometry. */
@Entity
@Table(name = "transit_route_points", indexes = @Index(name = "idx_route_points_route_sequence", columnList = "route_id, point_order"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransitRoutePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private TransitRoute route;

    @Column(name = "point_order", nullable = false)
    private int sequence;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "is_pass_through", nullable = false)
    private boolean passThrough;

    public TransitRoutePoint(int sequence, double latitude, double longitude, boolean passThrough) {
        this.sequence = sequence;
        this.latitude = latitude;
        this.longitude = longitude;
        this.passThrough = passThrough;
    }
}
