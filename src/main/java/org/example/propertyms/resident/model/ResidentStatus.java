package org.example.propertyms.resident.model;

public enum ResidentStatus {
    ACTIVE("在住"), MOVED_OUT("已迁出");

    private final String label;
    ResidentStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}


