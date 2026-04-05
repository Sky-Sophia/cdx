package org.example.propertyms.bill.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class FeeBill {
    private Long id;
    private String billNo;
    private Long unitId;
    private String unitNo;
    private String billingMonth;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

