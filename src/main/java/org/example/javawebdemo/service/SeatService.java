package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.dto.SeatRowView;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.Show;

public interface SeatService {
    List<SeatRowView> buildSeatMap(Long showId);

    void generateSeatsForShow(Show show);

    Order lockSeatsAndCreateOrder(Long showId, List<Long> seatIds, Long userId);

    void payOrder(Long orderId, Long userId, boolean allowAdmin);

    void refundOrder(Long orderId, Long userId, boolean allowAdmin);

    void releaseExpired();
}
