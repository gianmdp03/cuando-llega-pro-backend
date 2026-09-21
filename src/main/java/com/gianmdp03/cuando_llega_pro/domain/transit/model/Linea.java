package com.gianmdp03.cuando_llega_pro.domain.transit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A public transport line in the static MGP catalogue. */
@Entity
@Table(name = "lineas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Linea {

    @Id
    @Column(name = "codigo", nullable = false, length = 32)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 128)
    private String nombre;
}
