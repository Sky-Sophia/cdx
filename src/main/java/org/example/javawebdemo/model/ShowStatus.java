package org.example.javawebdemo.model;

public enum ShowStatus {
    SCHEDULED,
    CANCELED,
    ENDED;

    public String getLabel() {
        return switch (this) {
            case SCHEDULED -> "已排期";
            case CANCELED -> "已取消";
            case ENDED -> "已结束";
        };
    }
}
