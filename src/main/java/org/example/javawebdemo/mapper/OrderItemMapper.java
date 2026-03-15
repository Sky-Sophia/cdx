package org.example.javawebdemo.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.javawebdemo.model.OrderItem;

@Mapper
public interface OrderItemMapper {
    int insertBatch(@Param("items") List<OrderItem> items);

    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}
