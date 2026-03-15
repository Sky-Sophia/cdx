package org.example.javawebdemo.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.javawebdemo.mapper.ShowMapper;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.service.ShowService;
import org.springframework.stereotype.Service;

@Service
public class ShowServiceImpl implements ShowService {
    private final ShowMapper showMapper;

    public ShowServiceImpl(ShowMapper showMapper) {
        this.showMapper = showMapper;
    }

    @Override
    public List<Show> listWithFilters(LocalDate date, Long movieId, Long hallId) {
        return showMapper.findAllWithFilters(date, movieId, hallId);
    }

    @Override
    public Show getById(Long id) {
        return showMapper.findById(id);
    }

    @Override
    public void create(Show show) {
        showMapper.insert(show);
    }

    @Override
    public void update(Show show) {
        showMapper.update(show);
    }

    @Override
    public boolean hasShowsForMovie(Long movieId) {
        return showMapper.countByMovieId(movieId) > 0;
    }

    @Override
    public boolean hasShowsForHall(Long hallId) {
        return showMapper.countByHallId(hallId) > 0;
    }

    @Override
    public boolean hasConflict(Long hallId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        return showMapper.countConflicts(hallId, startTime, endTime, excludeId) > 0;
    }
}
