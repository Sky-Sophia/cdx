package org.example.javawebdemo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.javawebdemo.mapper.FeeBillMapper;
import org.example.javawebdemo.model.FeeBill;
import org.example.javawebdemo.service.FeeBillService;
import org.example.javawebdemo.util.CodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeBillServiceImpl implements FeeBillService {
    private final FeeBillMapper feeBillMapper;

    public FeeBillServiceImpl(FeeBillMapper feeBillMapper) {
        this.feeBillMapper = feeBillMapper;
    }

    @Override
    public List<FeeBill> list(String status, String billingMonth) {
        return feeBillMapper.findAll(status, billingMonth);
    }

    @Override
    public FeeBill findById(Long billId) {
        return feeBillMapper.findById(billId);
    }

    @Override
    @Transactional
    public void create(FeeBill bill) {
        if (bill == null
                || bill.getUnitId() == null
                || bill.getAmount() == null
                || bill.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || isBlank(bill.getBillingMonth())) {
            throw new IllegalArgumentException("请完整填写账单信息。");
        }
        bill.setBillNo(CodeGenerator.nextBillNo());
        if (bill.getPaidAmount() == null) {
            bill.setPaidAmount(BigDecimal.ZERO);
        }
        if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("实收金额不能为负数。");
        }
        if (bill.getPaidAmount().compareTo(bill.getAmount()) > 0) {
            throw new IllegalArgumentException("实收金额不能大于应收金额。");
        }

        String status = resolveStatus(bill.getAmount(), bill.getPaidAmount());
        bill.setStatus(status);
        bill.setPaidAt("PAID".equals(status) ? LocalDateTime.now() : null);
        feeBillMapper.insert(bill);
    }

    @Override
    @Transactional
    public void recordPayment(Long billId, BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("实收金额必须大于 0。");
        }
        FeeBill existing = feeBillMapper.findById(billId);
        if (existing == null) {
            throw new IllegalArgumentException("账单不存在。");
        }

        BigDecimal currentPaid = existing.getPaidAmount() == null ? BigDecimal.ZERO : existing.getPaidAmount();
        BigDecimal newPaidAmount = currentPaid.add(paidAmount);
        if (existing.getAmount() != null && newPaidAmount.compareTo(existing.getAmount()) > 0) {
            throw new IllegalArgumentException("累计实收金额不能大于应收金额。");
        }

        String status = resolveStatus(existing.getAmount(), newPaidAmount);
        LocalDateTime paidAt = "PAID".equals(status) ? LocalDateTime.now() : existing.getPaidAt();
        feeBillMapper.updatePayment(billId, newPaidAmount, status, paidAt);
    }

    private String resolveStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (totalAmount == null || paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "UNPAID";
        }
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return "PAID";
        }
        return "PARTIAL";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
