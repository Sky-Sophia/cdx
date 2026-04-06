package org.example.propertyms.bill.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import org.example.propertyms.bill.model.FeeBill;
import org.example.propertyms.bill.service.FeeBillService;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.util.ExcelExportHelper;
import org.example.propertyms.unit.service.PropertyUnitService;
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
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String billingMonth) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/management")
                .queryParam("tab", "bills");
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("billKeyword", keyword);
        }
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
        model.addAttribute("editing", false);
        model.addAttribute("remainingAmount", BigDecimal.ZERO);
        return "admin/bills/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        FeeBill bill = feeBillService.findById(id);
        if (bill == null) {
            redirectAttributes.addFlashAttribute("error", "账单不存在。");
            return RedirectUrls.MANAGEMENT_BILLS;
        }
        model.addAttribute("bill", bill);
        model.addAttribute("units", propertyUnitService.listSimple());
        model.addAttribute("editing", true);
        BigDecimal amount = bill.getAmount() == null ? BigDecimal.ZERO : bill.getAmount();
        BigDecimal paidAmount = bill.getPaidAmount() == null ? BigDecimal.ZERO : bill.getPaidAmount();
        model.addAttribute("remainingAmount", amount.subtract(paidAmount));
        return "admin/bills/form";
    }

    @PostMapping("/save")
    public String save(FeeBill bill, RedirectAttributes redirectAttributes) {
        try {
            feeBillService.create(bill);
            redirectAttributes.addFlashAttribute("success", "账单已创建。");
            return RedirectUrls.MANAGEMENT_BILLS;
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
            return RedirectUrls.MANAGEMENT_BILLS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/bills/edit/" + id;
        }
    }

    @GetMapping("/export")
    public void exportExcel(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            @RequestParam(required = false) String billingMonth,
                            HttpServletResponse response) throws IOException {
        var list = feeBillService.listAll(keyword, status, billingMonth);
        String[] headers = {"账单号", "房号", "账期", "应收", "实收", "到期日", "状态"};
        ExcelExportHelper.export(response, "账单列表", "账单列表", headers, list, (row, b) -> {
            row.getCell(0).setCellValue(b.getBillNo() != null ? b.getBillNo() : "");
            row.getCell(1).setCellValue(b.getUnitNo() != null ? b.getUnitNo() : "");
            row.getCell(2).setCellValue(b.getBillingMonth() != null ? b.getBillingMonth() : "");
            row.getCell(3).setCellValue(b.getAmount() != null ? b.getAmount().doubleValue() : 0);
            row.getCell(4).setCellValue(b.getPaidAmount() != null ? b.getPaidAmount().doubleValue() : 0);
            row.getCell(5).setCellValue(b.getDueDate() != null ? b.getDueDate().toString() : "");
            row.getCell(6).setCellValue(billStatusLabel(b.getStatus()));
        });
    }

    private String billStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "UNPAID" -> "未缴";
            case "PARTIAL" -> "部分已缴";
            case "OVERDUE" -> "逾期";
            case "PAID" -> "已缴清";
            default -> status;
        };
    }
}
