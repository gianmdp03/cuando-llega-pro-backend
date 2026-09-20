package com.gianmdp03.cuando_llega_pro.domain.preset.dto;

import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresetRequestDTO(
        @NotBlank(message = "Line code is required")
        String codigoLinea,

        @NotBlank(message = "Stop identifier is required")
        String identificadorParada,

        String bandera,

        @NotNull(message = "Config is required")
        @Valid
        PresetConfig config
) {
}
