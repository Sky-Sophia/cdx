package org.example.javawebdemo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class Resident {
    private Long id;
    private Long unitId;
    private String unitNo;
    private String name;
    private String phone;
    private String identityNo;
    private String residentType;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate moveInDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate moveOutDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
