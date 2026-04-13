package org.example.propertyms.workorder.model;

public enum Priority {
    LOW("低"), MEDIUM("中"), HIGH("高"), URGENT("紧急");

    private final String label;
    Priority(String label) { this.label = label; }
    public String getLabel() { return label; }
}


