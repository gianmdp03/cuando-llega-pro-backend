package com.gianmdp03.cuando_llega_pro.domain.preset.model;

public record PresetConfig(
        String alias,
        String icon,
        String color,
        String location
) {
    public PresetConfig(String alias, String icon, String color) {
        this(alias, icon, color, null);
    }
}
