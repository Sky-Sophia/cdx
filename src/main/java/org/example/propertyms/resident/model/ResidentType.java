package org.example.propertyms.resident.model;

public enum ResidentType {
    OWNER("业主"), TENANT("租户"), FAMILY("家属");

    private final String label;
    ResidentType(String label) { this.label = label; }
    public String getLabel() { return label; }
}

