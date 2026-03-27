package org.example.javawebdemo.model;

public enum HallType {
    NORMAL,
    IMAX;

    public String getLabel() {
        return switch (this) {
            case NORMAL -> "标准";
            case IMAX -> "IMAX";
        };
    }
}
