package org.example.propertyms.resident.model;

import lombok.Getter;

@Getter
public enum ResidentStatus {
    ACTIVE("在住"), MOVED_OUT("已迁出");

    private final String label;
    ResidentStatus(String label) { this.label = label; }
}


