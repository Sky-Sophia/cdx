package org.example.javawebdemo.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ShowSeat {
    private Long id;
    private Long showId;
    private Integer seatRow;
    private Integer seatCol;
    private String seatLabel;
    private SeatStatus status;
    private Long lockedByUserId;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
