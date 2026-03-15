package org.example.javawebdemo.model;

import java.util.List;
import lombok.Data;

@Data
public class SeatLayout {
    private Integer rows;
    private Integer cols;
    private List<String> disabled;
}
