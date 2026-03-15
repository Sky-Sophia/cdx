package org.example.javawebdemo.model;

public enum MovieStatus {
    ONLINE,
    OFFLINE;

    public String getLabel() {
        return switch (this) {
            case ONLINE -> "上映中";
            case OFFLINE -> "已下线";
        };
    }
}
