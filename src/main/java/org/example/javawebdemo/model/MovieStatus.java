package org.example.javawebdemo.model;

public enum MovieStatus {
    ONLINE,
    OFFLINE;

    public String getLabel() {
        return switch (this) {
            case ONLINE -> "上架";
            case OFFLINE -> "下架";
        };
    }
}
