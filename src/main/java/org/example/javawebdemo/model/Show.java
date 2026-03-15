package org.example.javawebdemo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Show {
    private Long id;
    private Long movieId;
    private Long hallId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private BigDecimal finalPrice;
    private ShowStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
