package org.example.javawebdemo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
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
            return resolveDefaultPosterByTitle();
        }

        String normalized = posterUrl.trim().replace("\\", "/");
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        if (normalized.startsWith("classpath:/static/")) {
            normalized = normalized.substring("classpath:/static/".length());
        } else if (normalized.startsWith("src/main/resources/static/")) {
            normalized = normalized.substring("src/main/resources/static/".length());
        } else if (normalized.startsWith("static.css/")) {
            normalized = "css/" + normalized.substring("static.css/".length());
        } else if (normalized.startsWith("static/")) {
            normalized = normalized.substring("static/".length());
        } else {
            int staticIndex = normalized.indexOf("/static/");
            if (staticIndex >= 0) {
                normalized = normalized.substring(staticIndex + "/static/".length());
            }
        }

        lower = normalized.toLowerCase(Locale.ROOT);
        int uploadsIndex = lower.indexOf("/uploads/");
        if (uploadsIndex >= 0) {
            normalized = normalized.substring(uploadsIndex + 1);
        } else if (!lower.startsWith("uploads/")) {
            int windowsUploadsIndex = lower.indexOf("uploads/");
            if (windowsUploadsIndex > 0) {
                normalized = normalized.substring(windowsUploadsIndex);
            }
        }

        if (isDefaultPosterPath(normalized)) {
            return resolveDefaultPosterByTitle();
        }

        if (normalized.startsWith("/")) {
            return normalized;
        }
        if (normalized.startsWith("png/")) {
            return "/css/" + normalized;
        }
        if (!normalized.contains("/")) {
            return "/css/png/" + normalized;
        }
        return "/" + normalized;
    }

    private boolean isDefaultPosterPath(String normalized) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        return lower.equals("/css/png/img.png")
                || lower.equals("css/png/img.png")
                || lower.equals("png/img.png")
                || lower.equals("img.png");
    }

    private String resolveDefaultPosterByTitle() {
        String movieTitle = title == null ? "" : title.trim();
        if (movieTitle.contains("星际穿越")) {
            return "/css/png/img1.png";
        }
        if (movieTitle.contains("银翼杀手")) {
            return "/css/png/img2.png";
        }
        if (movieTitle.contains("头号玩家")) {
            return "/css/png/img3.png";
        }
        if (movieTitle.contains("盗梦空间")) {
            return "/css/png/img4.png";
        }
        if (movieTitle.contains("沙丘2")) {
            return "/css/png/img5.png";
        }
        if (movieTitle.contains("奥本海默")) {
            return "/css/png/img6.png";
        }
        return DEFAULT_POSTER_URL;
    }
}
