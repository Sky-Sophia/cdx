package org.example.javawebdemo.service.impl;

import java.util.Collections;
import java.util.List;
import org.example.javawebdemo.dto.SeatRowView;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.service.SeatService;
import org.springframework.stereotype.Service;

@Service
public class SeatServiceImpl implements SeatService {

    @Override
    public List<SeatRowView> buildSeatMap(Long showId) {
        return Collections.emptyList();
    }

    @Override
    public void generateSeatsForShow(Show show) {
        // Legacy cinema module disabled.
    }

    @Override
    public Order lockSeatsAndCreateOrder(Long showId, List<Long> seatIds, Long userId) {
        throw new UnsupportedOperationException("Cinema order flow is disabled in property-management mode.");
    }

    @Override
    public void payOrder(Long orderId, Long userId, boolean allowAdmin) {
        throw new UnsupportedOperationException("Cinema order flow is disabled in property-management mode.");
    }

    @Override
    public void refundOrder(Long orderId, Long userId, boolean allowAdmin) {
        throw new UnsupportedOperationException("Cinema order flow is disabled in property-management mode.");
    }

    @Override
    public void releaseExpired() {
        // Legacy cinema module disabled.
    }
}
