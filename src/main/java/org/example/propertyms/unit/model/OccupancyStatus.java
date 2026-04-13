package org.example.propertyms.unit.model;

/**
 * 房屋入住状态枚举常量。
 */
public enum OccupancyStatus {
    SELF_OCCUPIED("自住"),
    RENTED("出租"),
    VACANT("空置"),
    UNSOLD("未售出");

    private final String label;

    OccupancyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}


