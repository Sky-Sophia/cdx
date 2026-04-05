package org.example.propertyms.bill.service;

import java.math.BigDecimal;
import java.util.List;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.common.dto.PageResult;

public interface FeeBillService {
    PageResult<FeeBill> listPaged(String status, String billingMonth, int page, int pageSize);

    FeeBill findById(Long billId);

    void create(FeeBill bill);

    void recordPayment(Long billId, BigDecimal paidAmount);

    long countDue();

    BigDecimal sumReceivable();

    BigDecimal sumReceived();

    List<FeeBill> findDueSoon(int limit);
}

