package com.gianmdp03.cuando_llega_pro.domain.preset.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PresetConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Record creation with complete fields")
    void recordCreationWithCompleteFields() {
        PresetConfig.ScheduleRange schedule = new PresetConfig.ScheduleRange("07:30", "19:00", Set.of("MON", "WED", "FRI"));
        PresetConfig.NotificationSettings notifications = new PresetConfig.NotificationSettings(true, 10, false);

        PresetConfig config = new PresetConfig("Work Line", "work", "indigo", schedule, notifications);

        assertThat(config.alias()).isEqualTo("Work Line");
        assertThat(config.icon()).isEqualTo("work");
        assertThat(config.color()).isEqualTo("indigo");
        assertThat(config.activeSchedule()).isEqualTo(schedule);
        assertThat(config.activeSchedule().startTime()).isEqualTo("07:30");
        assertThat(config.activeSchedule().endTime()).isEqualTo("19:00");
        assertThat(config.activeSchedule().activeDays()).containsExactlyInAnyOrder("MON", "WED", "FRI");
        assertThat(config.notificationSettings()).isEqualTo(notifications);
        assertThat(config.notificationSettings().notifyArrival()).isTrue();
        assertThat(config.notificationSettings().alertMinutesBefore()).isEqualTo(10);
        assertThat(config.notificationSettings().soundEnabled()).isFalse();
    }

    @Test
    @DisplayName("Record creation with null nested records")
    void recordCreationWithNullNestedRecords() {
        PresetConfig config = new PresetConfig("Minimal", "bus", "#0055FF", null, null);

        assertThat(config.alias()).isEqualTo("Minimal");
        assertThat(config.icon()).isEqualTo("bus");
        assertThat(config.color()).isEqualTo("#0055FF");
        assertThat(config.activeSchedule()).isNull();
        assertThat(config.notificationSettings()).isNull();
    }

    @Test
    @DisplayName("Jackson serializes and deserializes PresetConfig correctly")
    void jacksonSerializationRoundTrip() throws Exception {
        PresetConfig.ScheduleRange schedule = new PresetConfig.ScheduleRange("08:00", "18:00", Set.of("TUE", "THU"));
        PresetConfig.NotificationSettings notifications = new PresetConfig.NotificationSettings(true, 5, true);
        PresetConfig original = new PresetConfig("Home 511", "home", "#0055FF", schedule, notifications);

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"alias\":\"Home 511\"");
        assertThat(json).contains("\"icon\":\"home\"");
        assertThat(json).contains("\"color\":\"#0055FF\"");
        assertThat(json).contains("\"activeSchedule\"");
        assertThat(json).contains("\"notificationSettings\"");

        PresetConfig deserialized = objectMapper.readValue(json, PresetConfig.class);
        assertThat(deserialized).isEqualTo(original);
    }
}
