package org.example.javawebdemo.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.javawebdemo.dto.PageResult;
import org.example.javawebdemo.model.FeeBill;

public interface FeeBillService {
    List<FeeBill> list(String status, String billingMonth);

    PageResult<FeeBill> listPaged(String status, String billingMonth, int page, int pageSize);

    FeeBill findById(Long billId);

    void create(FeeBill bill);

    void recordPayment(Long billId, BigDecimal paidAmount);
}
