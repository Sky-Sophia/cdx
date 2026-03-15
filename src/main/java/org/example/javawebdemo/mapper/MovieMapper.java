package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.MovieStatus;

@Mapper
public interface MovieMapper {
    int insert(Movie movie);

    int update(Movie movie);

    int deleteById(@Param("id") Long id);

    Movie findById(@Param("id") Long id);

    List<Movie> findAll();

    List<Movie> findByStatus(@Param("status") MovieStatus status);
}
