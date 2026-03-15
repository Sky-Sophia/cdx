package org.example.javawebdemo.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long showSeatId;
    private String seatLabel;
    private BigDecimal price;
}
