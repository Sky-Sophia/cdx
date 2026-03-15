package org.example.javawebdemo.model;

public enum HallType {
    NORMAL,
    IMAX;

    public String getLabel() {
        return switch (this) {
            case NORMAL -> "普通厅";
            case IMAX -> "IMAX厅";
        };
    }
}
