package org.example.javawebdemo.model;

public enum OrderStatus {
    PENDING,
    PAID,
    REFUNDED,
    COMPLETED,
    CANCELED;

    public String getLabel() {
        return switch (this) {
            case PENDING -> "待支付";
            case PAID -> "已支付";
            case REFUNDED -> "已退款";
            case COMPLETED -> "已完成";
            case CANCELED -> "已取消";
        };
    }
}
