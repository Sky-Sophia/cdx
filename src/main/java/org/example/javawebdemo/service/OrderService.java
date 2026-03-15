package org.example.javawebdemo.service;

import java.util.List;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderStatus;

public interface OrderService {
    Order getById(Long id);

    Order getByOrderNo(String orderNo);

    List<Order> listByUser(Long userId);

    List<Order> listAll(Long userId, OrderStatus status);
}
