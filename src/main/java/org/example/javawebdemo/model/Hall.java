package org.example.javawebdemo.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Hall {
    private Long id;
    private String name;
    private Integer seatTotal;
    private HallType hallType;
    private String seatLayoutJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
