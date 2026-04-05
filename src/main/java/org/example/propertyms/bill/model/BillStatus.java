package org.example.propertyms.bill.model;

public enum BillStatus {
    UNPAID("未缴"), PARTIAL("部分缴费"), PAID("已缴清"), OVERDUE("逾期");

    private final String label;
    BillStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}

