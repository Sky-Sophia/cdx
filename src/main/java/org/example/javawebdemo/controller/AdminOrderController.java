package org.example.javawebdemo.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.example.javawebdemo.mapper.FeeBillMapper;
import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.model.FeeBill;
import org.example.javawebdemo.util.CodeGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/admin/bills")
public class AdminOrderController {
    private final FeeBillMapper feeBillMapper;
    private final PropertyUnitMapper propertyUnitMapper;

    public AdminOrderController(FeeBillMapper feeBillMapper, PropertyUnitMapper propertyUnitMapper) {
        this.feeBillMapper = feeBillMapper;
        this.propertyUnitMapper = propertyUnitMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String billingMonth,
                       Model model) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/management")
                .queryParam("tab", "bills");
        if (status != null && !status.isBlank()) {
            builder.queryParam("billStatus", status);
        }
        if (billingMonth != null && !billingMonth.isBlank()) {
            builder.queryParam("billBillingMonth", billingMonth);
        }
        return "redirect:" + builder.toUriString();
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        FeeBill bill = new FeeBill();
        bill.setStatus("UNPAID");
        bill.setPaidAmount(BigDecimal.ZERO);
        model.addAttribute("bill", bill);
        model.addAttribute("units", propertyUnitMapper.findAllSimple());
        return "admin/bill-form";
    }

    @PostMapping("/save")
    public String save(FeeBill bill, RedirectAttributes redirectAttributes) {
        if (bill.getUnitId() == null || bill.getAmount() == null || bill.getBillingMonth() == null || bill.getBillingMonth().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "请完整填写账单信息。");
            return "redirect:/admin/bills/new";
        }

        bill.setBillNo(CodeGenerator.nextBillNo());
        if (bill.getPaidAmount() == null) {
            bill.setPaidAmount(BigDecimal.ZERO);
        }
        if (bill.getStatus() == null || bill.getStatus().isBlank()) {
            bill.setStatus("UNPAID");
        }
        feeBillMapper.insert(bill);
        redirectAttributes.addFlashAttribute("success", "账单已创建。");
        return "redirect:/admin/management?tab=bills";
    }

    @PostMapping("/{id}/pay")
    public String markPaid(@PathVariable Long id,
                           @RequestParam BigDecimal paidAmount,
                           RedirectAttributes redirectAttributes) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "实收金额必须大于 0。");
            return "redirect:/admin/management?tab=bills";
        }

        feeBillMapper.updatePayment(id, paidAmount, "PAID", LocalDateTime.now());
        redirectAttributes.addFlashAttribute("success", "账单已登记为已缴。");
        return "redirect:/admin/management?tab=bills";
    }
}
