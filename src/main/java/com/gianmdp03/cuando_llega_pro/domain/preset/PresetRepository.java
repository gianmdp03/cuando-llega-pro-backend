package com.gianmdp03.cuando_llega_pro.domain.preset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PresetRepository extends JpaRepository<Preset, Long> {

    @Query("SELECT p FROM Preset p JOIN FETCH p.user WHERE p.user.id = :userId ORDER BY p.id ASC")
    List<Preset> findAllByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT p FROM Preset p JOIN FETCH p.user WHERE p.id = :id AND p.user.id = :userId")
    Optional<Preset> findByIdAndUserIdWithUser(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByUserIdAndCodigoLineaAndIdentificadorParadaAndBandera(
            Long userId,
            String codigoLinea,
            String identificadorParada,
            String bandera
    );
}
