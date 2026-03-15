package org.example.javawebdemo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.dto.SeatRowView;
import org.example.javawebdemo.dto.SeatView;
import org.example.javawebdemo.mapper.HallMapper;
import org.example.javawebdemo.mapper.OrderItemMapper;
import org.example.javawebdemo.mapper.OrderMapper;
import org.example.javawebdemo.mapper.ShowMapper;
import org.example.javawebdemo.mapper.ShowSeatMapper;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderItem;
import org.example.javawebdemo.model.OrderStatus;
import org.example.javawebdemo.model.SeatLayout;
import org.example.javawebdemo.model.SeatStatus;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.model.ShowStatus;
import org.example.javawebdemo.model.ShowSeat;
import org.example.javawebdemo.service.SeatService;
import org.example.javawebdemo.util.OrderNoUtils;
import org.example.javawebdemo.util.SeatLabelUtils;
import org.example.javawebdemo.util.SeatLayoutUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatServiceImpl implements SeatService {
    private final ShowSeatMapper showSeatMapper;
    private final ShowMapper showMapper;
    private final HallMapper hallMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Value("${app.seat-lock-minutes:15}")
    private int seatLockMinutes;

    @Value("${app.refund-before-minutes:60}")
    private int refundBeforeMinutes;

    public SeatServiceImpl(ShowSeatMapper showSeatMapper,
                           ShowMapper showMapper,
                           HallMapper hallMapper,
                           OrderMapper orderMapper,
                           OrderItemMapper orderItemMapper) {
        this.showSeatMapper = showSeatMapper;
        this.showMapper = showMapper;
        this.hallMapper = hallMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public List<SeatRowView> buildSeatMap(Long showId) {
        Show show = showMapper.findById(showId);
        if (show == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        showSeatMapper.releaseExpired(now);
        orderMapper.cancelExpired(now.minusMinutes(seatLockMinutes));
        Hall hall = hallMapper.findById(show.getHallId());
        SeatLayout layout = SeatLayoutUtils.parse(hall.getSeatLayoutJson());
        if (layout == null || layout.getRows() == null || layout.getCols() == null) {
            throw new IllegalArgumentException("影厅座位布局未配置");
        }
        List<ShowSeat> seats = showSeatMapper.findByShowId(showId);
        Map<String, ShowSeat> seatMap = new HashMap<>();
        for (ShowSeat seat : seats) {
            seatMap.put(seat.getSeatRow() + "-" + seat.getSeatCol(), seat);
        }
        List<String> disabled = layout.getDisabled();
        List<SeatRowView> rows = new ArrayList<>();
        for (int r = 1; r <= layout.getRows(); r++) {
            SeatRowView rowView = new SeatRowView();
            rowView.setRow(r);
            rowView.setRowLabel(SeatLabelUtils.rowLabel(r));
            List<SeatView> seatViews = new ArrayList<>();
            for (int c = 1; c <= layout.getCols(); c++) {
                SeatView view = new SeatView();
                view.setRow(r);
                view.setCol(c);
                String key = r + "-" + c;
                if (disabled != null && disabled.contains(key)) {
                    view.setStatus(SeatStatus.DISABLED);
                    view.setLabel("--");
                } else {
                    ShowSeat seat = seatMap.get(key);
                    if (seat != null) {
                        view.setId(seat.getId());
                        view.setLabel(seat.getSeatLabel());
                        view.setStatus(seat.getStatus());
                    } else {
                        view.setLabel(SeatLabelUtils.seatLabel(r, c));
                        view.setStatus(SeatStatus.DISABLED);
                    }
                }
                seatViews.add(view);
            }
            rowView.setSeats(seatViews);
            rows.add(rowView);
        }
        rows.sort(Comparator.comparingInt(SeatRowView::getRow));
        return rows;
    }

    @Override
    @Transactional
    public void generateSeatsForShow(Show show) {
        Hall hall = hallMapper.findById(show.getHallId());
        if (hall == null) {
            throw new IllegalArgumentException("影厅不存在");
        }
        SeatLayout layout = SeatLayoutUtils.parse(hall.getSeatLayoutJson());
        if (layout == null || layout.getRows() == null || layout.getCols() == null) {
            throw new IllegalArgumentException("影厅座位布局未配置");
        }
        List<String> disabled = layout.getDisabled();
        List<ShowSeat> seats = new ArrayList<>();
        for (int r = 1; r <= layout.getRows(); r++) {
            for (int c = 1; c <= layout.getCols(); c++) {
                String key = r + "-" + c;
                if (disabled != null && disabled.contains(key)) {
                    continue;
                }
                ShowSeat seat = new ShowSeat();
                seat.setShowId(show.getId());
                seat.setSeatRow(r);
                seat.setSeatCol(c);
                seat.setSeatLabel(SeatLabelUtils.seatLabel(r, c));
                seat.setStatus(SeatStatus.AVAILABLE);
                seats.add(seat);
            }
        }
        if (!seats.isEmpty()) {
            showSeatMapper.insertBatch(seats);
        }
    }

    @Override
    @Transactional
    public Order lockSeatsAndCreateOrder(Long showId, List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("璇烽€夋嫨搴т綅");
        }
        LocalDateTime now = LocalDateTime.now();
        showSeatMapper.releaseExpired(now);
        orderMapper.cancelExpired(now.minusMinutes(seatLockMinutes));
        Show show = showMapper.findById(showId);
        if (show == null) {
            throw new IllegalArgumentException("场次不存在");
        }
        if (show.getStatus() != null && show.getStatus() != ShowStatus.SCHEDULED) {
            throw new IllegalArgumentException("场次已取消或结束");
        }
        if (show.getStartTime() != null && show.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("场次已开始，无法购票");
        }
        List<ShowSeat> seats = showSeatMapper.findByIds(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("座位信息不完整");
        }
        for (ShowSeat seat : seats) {
            if (!seat.getShowId().equals(showId)) {
                throw new IllegalArgumentException("搴т綅涓嶅睘浜庤鍦烘");
            }
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                continue;
            }
            if (seat.getStatus() == SeatStatus.LOCKED
                    && seat.getLockedByUserId() != null
                    && seat.getLockedByUserId().equals(userId)
                    && seat.getLockedUntil() != null
                    && seat.getLockedUntil().isAfter(now)) {
                continue;
            }
            throw new IllegalArgumentException("座位不可用");
        }
        LocalDateTime lockUntil = now.plusMinutes(seatLockMinutes);
        showSeatMapper.updateStatusByIds(seatIds, SeatStatus.LOCKED, userId, lockUntil);

        Order order = new Order();
        order.setOrderNo(OrderNoUtils.generate());
        order.setUserId(userId);
        order.setShowId(showId);
        order.setStatus(OrderStatus.PENDING);
        BigDecimal total = show.getFinalPrice().multiply(new BigDecimal(seatIds.size()));
        order.setTotalPrice(total);
        orderMapper.insert(order);

        List<OrderItem> items = new ArrayList<>();
        for (ShowSeat seat : seats) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setShowSeatId(seat.getId());
            item.setSeatLabel(seat.getSeatLabel());
            item.setPrice(show.getFinalPrice());
            items.add(item);
        }
        orderItemMapper.insertBatch(items);
        return order;
    }

    @Override
    @Transactional
    public void payOrder(Long orderId, Long userId, boolean allowAdmin) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!allowAdmin && !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作订单");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("订单状态不可支付");
        }
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        List<Long> seatIds = new ArrayList<>();
        for (OrderItem item : items) {
            seatIds.add(item.getShowSeatId());
        }
        List<ShowSeat> seats = showSeatMapper.findByIds(seatIds);
        for (ShowSeat seat : seats) {
            if (seat.getStatus() != SeatStatus.LOCKED) {
                throw new IllegalArgumentException("閿佸骇宸插け鏁堬紝璇烽噸鏂伴€夊骇");
            }
        }
        showSeatMapper.updateStatusByIds(seatIds, SeatStatus.SOLD, null, null);
        orderMapper.updateStatus(orderId, OrderStatus.PAID, LocalDateTime.now(), null);
    }

    @Override
    @Transactional
    public void refundOrder(Long orderId, Long userId, boolean allowAdmin) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!allowAdmin && !order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限操作订单");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("订单状态不可退款");
        }
        Show show = showMapper.findById(order.getShowId());
        if (show.getStartTime().minusMinutes(refundBeforeMinutes).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("开场前" + refundBeforeMinutes + "分钟内不可退款");
        }
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        List<Long> seatIds = new ArrayList<>();
        for (OrderItem item : items) {
            seatIds.add(item.getShowSeatId());
        }
        showSeatMapper.updateStatusByIds(seatIds, SeatStatus.AVAILABLE, null, null);
        orderMapper.updateStatus(orderId, OrderStatus.REFUNDED, null, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void releaseExpired() {
        LocalDateTime now = LocalDateTime.now();
        showSeatMapper.releaseExpired(now);
        orderMapper.cancelExpired(now.minusMinutes(seatLockMinutes));
    }
}
