package com.gianmdp03.cuando_llega_pro.domain.preset.dto;

import com.gianmdp03.cuando_llega_pro.domain.preset.Preset;
import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;

public record PresetListDTO(
        Long id,
        String codigoLinea,
        String identificadorParada,
        String bandera,
        String alias,
        String icon,
        String color
) {
    public static PresetListDTO fromEntity(Preset preset) {
        PresetConfig config = preset.getConfig();
        return new PresetListDTO(
                preset.getId(),
                preset.getCodigoLinea(),
                preset.getIdentificadorParada(),
                preset.getBandera(),
                config != null ? config.alias() : null,
                config != null ? config.icon() : null,
                config != null ? config.color() : null
        );
    }
}
