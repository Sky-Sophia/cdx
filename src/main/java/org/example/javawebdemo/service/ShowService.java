package org.example.javawebdemo.service;

import java.time.LocalDate;
import java.util.List;
import org.example.javawebdemo.model.Show;

public interface ShowService {
    List<Show> listWithFilters(LocalDate date, Long movieId, Long hallId);

    Show getById(Long id);

    void create(Show show);

    void update(Show show);

    boolean hasShowsForMovie(Long movieId);

    boolean hasShowsForHall(Long hallId);

    boolean hasConflict(Long hallId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, Long excludeId);
}
