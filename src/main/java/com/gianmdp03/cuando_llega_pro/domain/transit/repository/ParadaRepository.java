package com.gianmdp03.cuando_llega_pro.domain.transit.repository;

import com.gianmdp03.cuando_llega_pro.domain.transit.model.Parada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParadaRepository extends JpaRepository<Parada, String> {

    @Query("""
            select distinct parada
            from Parada parada
            join parada.lineas paradaLinea
            where paradaLinea.linea.codigo = :codigoLinea
              and paradaLinea.bandera = :bandera
            order by parada.descripcion asc
            """)
    List<Parada> findByCodigoLineaAndBandera(
            @Param("codigoLinea") String codigoLinea,
            @Param("bandera") String bandera
    );

    @Query("""
            select distinct parada
            from Parada parada
            left join fetch parada.lineas paradaLinea
            left join fetch paradaLinea.linea
            where parada.identificador = :identificador
            """)
    Optional<Parada> findByIdentificadorWithLineas(@Param("identificador") String identificador);
}
