package org.example.javawebdemo.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.javawebdemo.model.FeeBill;

public interface FeeBillService {
    List<FeeBill> list(String status, String billingMonth);

    FeeBill findById(Long billId);

    void create(FeeBill bill);

    void recordPayment(Long billId, BigDecimal paidAmount);
}
