package com.gianmdp03.cuando_llega_pro.domain.transit.repository;

import com.gianmdp03.cuando_llega_pro.domain.transit.model.ParadaLinea;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.SentidoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParadaLineaRepository extends JpaRepository<ParadaLinea, Long> {

    @Query("""
            select distinct paradaLinea.bandera
            from ParadaLinea paradaLinea
            where paradaLinea.linea.codigo = :codigoLinea
            order by paradaLinea.bandera asc
            """)
    List<String> findDistinctBanderasByLineaCodigo(@Param("codigoLinea") String codigoLinea);

    @Query("""
            select distinct new com.gianmdp03.cuando_llega_pro.domain.transit.dto.SentidoDTO(
                paradaLinea.bandera, paradaLinea.banderaAmpliada
            )
            from ParadaLinea paradaLinea
            where paradaLinea.linea.codigo = :codigoLinea
            order by paradaLinea.bandera asc, paradaLinea.banderaAmpliada asc
            """)
    List<SentidoDTO> findDistinctSentidosByLineaCodigo(@Param("codigoLinea") String codigoLinea);

    Optional<ParadaLinea> findByParadaIdentificadorAndLineaCodigoAndBandera(
            String identificador,
            String codigoLinea,
            String bandera
    );
}
