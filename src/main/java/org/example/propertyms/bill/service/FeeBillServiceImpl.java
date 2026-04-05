package org.example.propertyms.bill.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.propertyms.bill.mapper.FeeBillMapper;
import org.example.propertyms.bill.model.BillStatus;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.common.dto.PageResult;
import org.example.propertyms.common.util.CodeGenerator;
import org.example.propertyms.common.util.StringHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeBillServiceImpl implements FeeBillService {
    private final FeeBillMapper feeBillMapper;

    public FeeBillServiceImpl(FeeBillMapper feeBillMapper) {
        this.feeBillMapper = feeBillMapper;
    }

    @Override
    public PageResult<FeeBill> listPaged(String status, String billingMonth, int page, int pageSize) {
        long total = feeBillMapper.count(status, billingMonth);
        int offset = PageResult.calcOffset(page, pageSize);
        List<FeeBill> items = feeBillMapper.findAllPaged(status, billingMonth, offset, pageSize);
        return new PageResult<>(items, page, pageSize, total);
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
                || StringHelper.isBlank(bill.getBillingMonth())) {
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
        bill.setPaidAt(BillStatus.PAID.name().equals(status) ? LocalDateTime.now() : null);
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
        LocalDateTime paidAt = BillStatus.PAID.name().equals(status) ? LocalDateTime.now() : existing.getPaidAt();
        feeBillMapper.updatePayment(billId, newPaidAmount, status, paidAt);
    }

    @Override
    public long countDue() {
        return feeBillMapper.countDue();
    }

    @Override
    public BigDecimal sumReceivable() {
        BigDecimal v = feeBillMapper.sumReceivable();
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public BigDecimal sumReceived() {
        BigDecimal v = feeBillMapper.sumReceived();
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public List<FeeBill> findDueSoon(int limit) {
        return feeBillMapper.findDueSoon(limit);
    }

    private String resolveStatus(BigDecimal totalAmount, BigDecimal paidAmount) {
        if (totalAmount == null || paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BillStatus.UNPAID.name();
        }
        if (paidAmount.compareTo(totalAmount) >= 0) {
            return BillStatus.PAID.name();
        }
        return BillStatus.PARTIAL.name();
    }
}

