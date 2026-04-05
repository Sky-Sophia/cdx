package org.example.propertyms.workorder.model;

public enum WorkOrderStatus {
    OPEN("待处理"), IN_PROGRESS("处理中"), DONE("已完成"), CLOSED("已关闭");

    private final String label;
    WorkOrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}

