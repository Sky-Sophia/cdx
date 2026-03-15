package org.example.javawebdemo.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.SeatStatus;
import org.example.javawebdemo.model.ShowSeat;

@Mapper
public interface ShowSeatMapper {
    int insertBatch(@Param("seats") List<ShowSeat> seats);

    List<ShowSeat> findByShowId(@Param("showId") Long showId);

    List<ShowSeat> findByIds(@Param("ids") List<Long> ids);

    int updateStatusByIds(@Param("ids") List<Long> ids,
                          @Param("status") SeatStatus status,
                          @Param("lockedBy") Long lockedBy,
                          @Param("lockedUntil") LocalDateTime lockedUntil);

    int releaseExpired(@Param("now") LocalDateTime now);
}
