package org.example.propertyms.unit.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PropertyUnit {
    private Long id;
    private Long buildingId;
    private String buildingName;
    private String unitNo;
    private Integer floorNo;
    private BigDecimal areaM2;
    private String occupancyStatus;
    private String ownerName;
    private String ownerPhone;
    private Long ownerResidentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

