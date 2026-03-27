package org.example.javawebdemo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Movie {
    private static final String DEFAULT_POSTER_URL = "/css/png/img.png";

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

    public String getResolvedPosterUrl() {
        if (posterUrl == null || posterUrl.isBlank()) {
            return DEFAULT_POSTER_URL;
        }
        if (posterUrl.startsWith("http://") || posterUrl.startsWith("https://") || posterUrl.startsWith("/")) {
            return posterUrl;
        }
        return "/" + posterUrl.replace("\\", "/");
    }
}
