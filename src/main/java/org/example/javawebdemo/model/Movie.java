package org.example.javawebdemo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Movie {
    private Long id;
    private String title;
    private String director;
    private String actors;
    private String genre;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;
    private String synopsis;
    private MovieStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
