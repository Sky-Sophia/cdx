package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderStatus;

@Mapper
public interface OrderMapper {
    int insert(Order order);

    Order findById(@Param("id") Long id);

    Order findByOrderNo(@Param("orderNo") String orderNo);

    List<Order> findByUserId(@Param("userId") Long userId);

    List<Order> findAllWithFilters(@Param("userId") Long userId,
                                   @Param("status") OrderStatus status);

    int updateStatus(@Param("id") Long id,
                     @Param("status") OrderStatus status,
                     @Param("paidAt") java.time.LocalDateTime paidAt,
                     @Param("refundedAt") java.time.LocalDateTime refundedAt);

    int cancelExpired(@Param("before") java.time.LocalDateTime before);
}
