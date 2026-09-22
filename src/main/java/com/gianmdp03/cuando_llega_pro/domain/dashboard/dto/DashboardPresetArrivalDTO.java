package com.gianmdp03.cuando_llega_pro.domain.dashboard.dto;

import com.gianmdp03.cuando_llega_pro.domain.preset.model.PresetConfig;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;

/**
 * Data transfer object representing a preset combined with its real-time arrival telemetry.
 *
 * @param presetId            preset identifier
 * @param codigoLinea         transit line code
 * @param identificadorParada bus stop identifier
 * @param bandera             route branch / variant
 * @param config              preset visual and notification configuration
 * @param telemetry           real-time or extrapolated arrival telemetry
 * @param error               safe user-facing reason when telemetry is unavailable
 */
public record DashboardPresetArrivalDTO(
        Long presetId,
        String codigoLinea,
        String identificadorParada,
        String bandera,
        PresetConfig config,
        ArrivalResponseDTO telemetry,
        String error
) {
    public DashboardPresetArrivalDTO(
            Long presetId,
            String codigoLinea,
            String identificadorParada,
            String bandera,
            PresetConfig config,
            ArrivalResponseDTO telemetry
    ) {
        this(presetId, codigoLinea, identificadorParada, bandera, config, telemetry, null);
    }
}
