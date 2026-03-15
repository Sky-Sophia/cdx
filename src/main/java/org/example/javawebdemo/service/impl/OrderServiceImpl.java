package org.example.javawebdemo.service.impl;

import java.util.List;
import org.example.javawebdemo.mapper.OrderMapper;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderStatus;
import org.example.javawebdemo.service.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Order getById(Long id) {
        return orderMapper.findById(id);
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.findByOrderNo(orderNo);
    }

    @Override
    public List<Order> listByUser(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Override
    public List<Order> listAll(Long userId, OrderStatus status) {
        return orderMapper.findAllWithFilters(userId, status);
    }
}
