package com.gianmdp03.cuando_llega_pro.domain.preset.dto;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;

import java.time.Instant;

public record PresetDetailDTO(
        Long id,
        Long userId,
        String codigoLinea,
        String identificadorParada,
        String bandera,
        PresetConfig config,
        Instant createdAt
) {
    public static PresetDetailDTO fromEntity(Preset preset) {
        Long userId = (preset.getUser() != null) ? preset.getUser().getId() : null;
        return new PresetDetailDTO(
                preset.getId(),
                userId,
                preset.getCodigoLinea(),
                preset.getIdentificadorParada(),
                preset.getBandera(),
                preset.getConfig(),
                preset.getCreatedAt()
        );
    }
}
