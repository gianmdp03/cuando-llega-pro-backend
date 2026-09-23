package com.gianmdp03.cuando_llega_pro.domain.preset.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PresetConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Record creation with complete fields")
    void recordCreationWithCompleteFields() {
        PresetConfig config = new PresetConfig("Work Line", "work", "indigo", "Av. Colón 123");

        assertThat(config.alias()).isEqualTo("Work Line");
        assertThat(config.icon()).isEqualTo("work");
        assertThat(config.color()).isEqualTo("indigo");
        assertThat(config.location()).isEqualTo("Av. Colón 123");
    }

    @Test
    @DisplayName("Three-field constructor leaves location empty")
    void threeFieldConstructorLeavesLocationEmpty() {
        PresetConfig config = new PresetConfig("Minimal", "bus", "#0055FF");

        assertThat(config.alias()).isEqualTo("Minimal");
        assertThat(config.icon()).isEqualTo("bus");
        assertThat(config.color()).isEqualTo("#0055FF");
        assertThat(config.location()).isNull();
    }

    @Test
    @DisplayName("Jackson serializes and deserializes PresetConfig correctly")
    void jacksonSerializationRoundTrip() throws Exception {
        PresetConfig original = new PresetConfig("Home 511", "home", "#0055FF", "Casa");

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"alias\":\"Home 511\"");
        assertThat(json).contains("\"icon\":\"home\"");
        assertThat(json).contains("\"color\":\"#0055FF\"");
        assertThat(json).contains("\"location\":\"Casa\"");

        PresetConfig deserialized = objectMapper.readValue(json, PresetConfig.class);
        assertThat(deserialized).isEqualTo(original);
    }
}
