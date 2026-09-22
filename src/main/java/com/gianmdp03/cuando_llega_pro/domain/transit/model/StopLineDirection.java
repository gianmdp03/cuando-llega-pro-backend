package com.gianmdp03.cuando_llega_pro.domain.transit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Associates a physical stop with one line and its travel direction. */
@Entity
@Table(
        name = "stop_directions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stop_line_direction",
                columnNames = {"stop_identifier", "line_code", "direction"}
        ),
        indexes = {
                @Index(name = "idx_stop_directions_line_direction", columnList = "line_code, direction"),
                @Index(name = "idx_stop_directions_stop", columnList = "stop_identifier")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StopLineDirection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stop_identifier", nullable = false)
    private TransitStop stop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_code", nullable = false)
    private TransitLine line;

    @Column(name = "direction", nullable = false, length = 255)
    private String direction;

    @Column(name = "expanded_direction", length = 500)
    private String expandedDirection;

    /** Zero-based position of this stop in the route sequence for the given line and direction. */
    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    public StopLineDirection(TransitLine line, String direction, String expandedDirection) {
        this.line = line;
        this.direction = direction;
        this.expandedDirection = expandedDirection;
    }

    public StopLineDirection(TransitLine line, String direction, String expandedDirection, int stopOrder) {
        this.line = line;
        this.direction = direction;
        this.expandedDirection = expandedDirection;
        this.stopOrder = stopOrder;
    }
}
