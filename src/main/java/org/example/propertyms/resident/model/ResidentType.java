package org.example.propertyms.resident.model;

import lombok.Getter;

@Getter
public enum ResidentType {
    OWNER("业主"),
    TENANT("租客");

    private final String label;

    ResidentType(String label) {
        this.label = label;
    }

    public static ResidentType from(String value) {
        if (value == null || value.isBlank()) {
            return OWNER;
        }
        for (ResidentType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("住户类型不正确。");
    }
}
