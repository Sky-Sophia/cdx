package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.mapper.MovieMapper;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.MovieStatus;
import org.example.javawebdemo.service.MovieService;
import org.springframework.stereotype.Service;

@Service
public class MovieServiceImpl implements MovieService {
    private final MovieMapper movieMapper;

    public MovieServiceImpl(MovieMapper movieMapper) {
        this.movieMapper = movieMapper;
    }

    @Override
    public List<Movie> listAll() {
        return movieMapper.findAll();
    }

    @Override
    public List<Movie> listByStatus(MovieStatus status) {
        return movieMapper.findByStatus(status);
    }

    @Override
    public Movie getById(Long id) {
        return movieMapper.findById(id);
    }

    @Override
    public void create(Movie movie) {
        movieMapper.insert(movie);
    }

    @Override
    public void update(Movie movie) {
        movieMapper.update(movie);
    }

    @Override
    public void delete(Long id) {
        movieMapper.deleteById(id);
    }
}
