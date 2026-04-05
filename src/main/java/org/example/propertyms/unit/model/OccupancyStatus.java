package org.example.propertyms.unit.model;

/**
 * 房屋入住状态枚举常量。
 */
public enum OccupancyStatus {
    VACANT("空置"),
    OCCUPIED("已入住"),
    MAINTENANCE("维护中");

    private final String label;

    OccupancyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

