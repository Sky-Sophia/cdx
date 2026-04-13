package org.example.propertyms.bill.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.example.propertyms.bill.mapper.FeeBillMapper;
import org.example.propertyms.bill.model.FeeBill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeeBillServiceImplTest {

    @Mock
    private FeeBillMapper feeBillMapper;

    @InjectMocks
    private FeeBillServiceImpl feeBillService;

    @Test
    void create_shouldDefaultToUnpaidWhenNoPayment() {
        FeeBill bill = new FeeBill();
        bill.setUnitId(1L);
        bill.setBillingMonth("2026-04");
        bill.setAmount(new BigDecimal("100.00"));

        feeBillService.create(bill);

        assertEquals("UNPAID", bill.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(bill.getPaidAmount()));
        assertNotNull(bill.getBillNo());
        verify(feeBillMapper).insert(bill);
    }

    @Test
    void create_shouldMarkAsPaidWhenFullyPaid() {
        FeeBill bill = new FeeBill();
        bill.setUnitId(2L);
        bill.setBillingMonth("2026-04");
        bill.setAmount(new BigDecimal("88.00"));
        bill.setPaidAmount(new BigDecimal("88.00"));

        feeBillService.create(bill);

        assertEquals("PAID", bill.getStatus());
        assertNotNull(bill.getPaidAt());
        verify(feeBillMapper).insert(bill);
    }

    @Test
    void recordPayment_shouldMarkAsPartialWhenNotFullyPaid() {
        FeeBill existing = new FeeBill();
        existing.setId(10L);
        existing.setAmount(new BigDecimal("100.00"));
        existing.setPaidAmount(new BigDecimal("20.00"));
        existing.setPaidAt(null);
        when(feeBillMapper.findById(10L)).thenReturn(existing);

        feeBillService.recordPayment(10L, new BigDecimal("30.00"));

        verify(feeBillMapper).updatePayment(
                eq(10L),
                eq(new BigDecimal("50.00")),
                eq("PARTIAL"),
                eq(null));
    }

    @Test
    void recordPayment_shouldRejectOverPayment() {
        FeeBill existing = new FeeBill();
        existing.setId(20L);
        existing.setAmount(new BigDecimal("100.00"));
        existing.setPaidAmount(new BigDecimal("90.00"));
        existing.setPaidAt(LocalDateTime.now());
        when(feeBillMapper.findById(20L)).thenReturn(existing);

        assertThrows(
                IllegalArgumentException.class,
                () -> feeBillService.recordPayment(20L, new BigDecimal("20.00")));

        verify(feeBillMapper, never()).updatePayment(eq(20L), any(), any(), any());
    }
}



