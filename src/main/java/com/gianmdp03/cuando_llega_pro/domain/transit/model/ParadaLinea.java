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
        name = "parada_lineas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_parada_linea_bandera",
                columnNames = {"parada_identificador", "linea_codigo", "bandera"}
        ),
        indexes = {
                @Index(name = "idx_parada_lineas_linea_bandera", columnList = "linea_codigo, bandera"),
                @Index(name = "idx_parada_lineas_parada", columnList = "parada_identificador")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ParadaLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parada_identificador", nullable = false)
    private Parada parada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_codigo", nullable = false)
    private Linea linea;

    @Column(name = "bandera", nullable = false, length = 255)
    private String bandera;

    @Column(name = "bandera_ampliada", length = 500)
    private String banderaAmpliada;

    public ParadaLinea(Linea linea, String bandera, String banderaAmpliada) {
        this.linea = linea;
        this.bandera = bandera;
        this.banderaAmpliada = banderaAmpliada;
    }
}
