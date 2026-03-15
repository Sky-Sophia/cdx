package org.example.javawebdemo.dto;

import org.example.javawebdemo.model.SeatStatus;
import lombok.Data;

@Data
public class SeatView {
    private Long id;
    private int row;
    private int col;
    private String label;
    private SeatStatus status;
}
