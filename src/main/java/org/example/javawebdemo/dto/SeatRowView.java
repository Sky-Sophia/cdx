package org.example.javawebdemo.dto;

import java.util.List;
import lombok.Data;

@Data
public class SeatRowView {
    private int row;
    private String rowLabel;
    private List<SeatView> seats;
}
