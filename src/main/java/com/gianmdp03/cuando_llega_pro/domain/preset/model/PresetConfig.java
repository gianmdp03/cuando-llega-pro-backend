package com.gianmdp03.cuando_llega_pro.domain.preset.model;

import java.util.Set;

public record PresetConfig(
        String alias,
        String icon,
        String color,
        ScheduleRange activeSchedule,
        NotificationSettings notificationSettings
) {
    public record ScheduleRange(
            String startTime,
            String endTime,
            Set<String> activeDays
    ) {
    }

    public record NotificationSettings(
            boolean notifyArrival,
            Integer alertMinutesBefore,
            boolean soundEnabled
    ) {
    }
}
