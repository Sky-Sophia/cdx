package org.example.javawebdemo.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Building {
    private Long id;
    private String name;
    private String code;
    private String address;
    private Integer floorCount;
    private Integer unitCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

