package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.MovieStatus;

public interface MovieService {
    List<Movie> listAll();

    List<Movie> listByStatus(MovieStatus status);

    Movie getById(Long id);

    void create(Movie movie);

    void update(Movie movie);

    void delete(Long id);
}
