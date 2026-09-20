package com.gianmdp03.cuando_llega_pro.domain.preset.controller;

import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetDetailDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetListDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.dto.PresetRequestDTO;
import com.gianmdp03.cuando_llega_pro.domain.preset.service.PresetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * REST controller for managing authenticated user presets.
 * Enforces strict DTO Triad boundary: never leaks domain entities.
 */
@RestController
@RequestMapping("/api/v1/presets")
public class PresetController {

    private final PresetService presetService;

    public PresetController(PresetService presetService) {
        this.presetService = presetService;
    }

    /**
     * Lists all presets belonging to the authenticated user.
     *
     * @param principal authenticated user principal
     * @return 200 OK with list of preset summaries
     */
    @GetMapping
    public ResponseEntity<List<PresetListDTO>> getPresets(Principal principal) {
        List<PresetListDTO> presets = presetService.getPresetsForUser(principal.getName());
        return ResponseEntity.ok(presets);
    }

    /**
     * Retrieves a single preset by ID for the authenticated user.
     *
     * @param id        preset ID
     * @param principal authenticated user principal
     * @return 200 OK with preset detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<PresetDetailDTO> getPresetById(
            @PathVariable Long id,
            Principal principal
    ) {
        PresetDetailDTO preset = presetService.getPresetByIdForUser(id, principal.getName());
        return ResponseEntity.ok(preset);
    }

    /**
     * Creates a new preset for the authenticated user.
     *
     * @param request   validated preset payload
     * @param principal authenticated user principal
     * @return 201 Created with created preset detail
     */
    @PostMapping
    public ResponseEntity<PresetDetailDTO> createPreset(
            @Valid @RequestBody PresetRequestDTO request,
            Principal principal
    ) {
        PresetDetailDTO created = presetService.createPresetForUser(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing preset owned by the authenticated user.
     *
     * @param id        preset ID
     * @param request   validated preset update payload
     * @param principal authenticated user principal
     * @return 200 OK with updated preset detail
     */
    @PutMapping("/{id}")
    public ResponseEntity<PresetDetailDTO> updatePreset(
            @PathVariable Long id,
            @Valid @RequestBody PresetRequestDTO request,
            Principal principal
    ) {
        PresetDetailDTO updated = presetService.updatePresetForUser(id, principal.getName(), request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a preset owned by the authenticated user.
     *
     * @param id        preset ID
     * @param principal authenticated user principal
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePreset(
            @PathVariable Long id,
            Principal principal
    ) {
        presetService.deletePresetForUser(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
