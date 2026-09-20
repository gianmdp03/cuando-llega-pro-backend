package com.gianmdp03.cuando_llega_pro.domain.dashboard.dto;

import java.time.Instant;
import java.util.List;

/**
 * Data transfer object encapsulating the full aggregated dashboard response for a user.
 *
 * @param userEmail    authenticated user email address
 * @param generatedAt  timestamp when the dashboard snapshot was assembled
 * @param totalPresets total count of presets evaluated
 * @param presets      list of presets with their corresponding arrival telemetries
 */
public record DashboardResponseDTO(
        String userEmail,
        Instant generatedAt,
        int totalPresets,
        List<DashboardPresetArrivalDTO> presets
) {
    public DashboardResponseDTO {
        if (presets == null) {
            presets = List.of();
        } else {
            presets = List.copyOf(presets);
        }
    }
}
