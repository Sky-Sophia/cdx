package org.example.javawebdemo.controller;

import java.math.BigDecimal;
import org.example.javawebdemo.model.FeeBill;
import org.example.javawebdemo.service.FeeBillService;
import org.example.javawebdemo.service.PropertyUnitService;
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
public class AdminBillController {
    private final FeeBillService feeBillService;
    private final PropertyUnitService propertyUnitService;

    public AdminBillController(FeeBillService feeBillService, PropertyUnitService propertyUnitService) {
        this.feeBillService = feeBillService;
        this.propertyUnitService = propertyUnitService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String billingMonth) {
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
        model.addAttribute("units", propertyUnitService.listSimple());
        return "admin/bills/form";
    }

    @PostMapping("/save")
    public String save(FeeBill bill, RedirectAttributes redirectAttributes) {
        try {
            feeBillService.create(bill);
            redirectAttributes.addFlashAttribute("success", "账单已创建。");
            return "redirect:/admin/management?tab=bills";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/bills/new";
        }
    }

    @PostMapping("/{id}/pay")
    public String markPaid(@PathVariable Long id,
                           @RequestParam BigDecimal paidAmount,
                           RedirectAttributes redirectAttributes) {
        try {
            feeBillService.recordPayment(id, paidAmount);
            redirectAttributes.addFlashAttribute("success", "账单收款已登记。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/management?tab=bills";
    }
}
