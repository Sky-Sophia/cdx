package org.example.javawebdemo.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardStats {
    private long buildingCount;
    private long unitCount;
    private long residentCount;
    private long openOrderCount;
    private long dueBillCount;
    private BigDecimal totalReceivable;
    private BigDecimal totalReceived;
}
