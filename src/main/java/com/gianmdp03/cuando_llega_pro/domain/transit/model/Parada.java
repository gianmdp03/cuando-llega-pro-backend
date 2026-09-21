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
@Table(name = "paradas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parada {

    @Id
    @Column(name = "identificador", nullable = false, length = 100)
    private String identificador;

    @Column(name = "codigo", length = 100)
    private String codigo;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @OneToMany(mappedBy = "parada", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ParadaLinea> lineas = new LinkedHashSet<>();

    public Parada(String identificador, String codigo, String descripcion, Double latitud, Double longitud) {
        this.identificador = identificador;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public void addLinea(ParadaLinea paradaLinea) {
        lineas.add(paradaLinea);
        paradaLinea.setParada(this);
    }

    public void removeLinea(ParadaLinea paradaLinea) {
        lineas.remove(paradaLinea);
        paradaLinea.setParada(null);
    }
}
