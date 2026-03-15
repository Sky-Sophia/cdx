package org.example.javawebdemo.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.Show;

@Mapper
public interface ShowMapper {
    int insert(Show show);

    int update(Show show);

    Show findById(@Param("id") Long id);

    List<Show> findAllWithFilters(@Param("date") LocalDate date,
                                  @Param("movieId") Long movieId,
                                  @Param("hallId") Long hallId);

    int countByMovieId(@Param("movieId") Long movieId);

    int countByHallId(@Param("hallId") Long hallId);

    int countConflicts(@Param("hallId") Long hallId,
                       @Param("startTime") java.time.LocalDateTime startTime,
                       @Param("endTime") java.time.LocalDateTime endTime,
                       @Param("excludeId") Long excludeId);
}
