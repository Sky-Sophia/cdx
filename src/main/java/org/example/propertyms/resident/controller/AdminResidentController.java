package org.example.propertyms.resident.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.util.ExcelExportHelper;
import org.example.propertyms.resident.model.Resident;
import org.example.propertyms.resident.service.ResidentService;
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
@RequestMapping("/admin/residents")
public class AdminResidentController {
    private final ResidentService residentService;
    private final PropertyUnitService propertyUnitService;

    public AdminResidentController(ResidentService residentService, PropertyUnitService propertyUnitService) {
        this.residentService = residentService;
        this.propertyUnitService = propertyUnitService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/management")
                .queryParam("tab", "residents");
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("residentKeyword", keyword);
        }
        if (status != null && !status.isBlank()) {
            builder.queryParam("residentStatus", status);
        }
        return "redirect:" + builder.toUriString();
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("resident", new Resident());
        model.addAttribute("units", propertyUnitService.listSimple());
        model.addAttribute("editing", false);
        return "admin/residents/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Resident resident = residentService.findById(id);
        if (resident == null) {
            redirectAttributes.addFlashAttribute("error", "住户不存在。");
            return RedirectUrls.MANAGEMENT_RESIDENTS;
        }
        model.addAttribute("resident", resident);
        model.addAttribute("units", propertyUnitService.listSimple());
        model.addAttribute("editing", true);
        return "admin/residents/form";
    }

    @PostMapping("/save")
    public String save(Resident resident, RedirectAttributes redirectAttributes) {
        boolean creating = resident.getId() == null;
        try {
            residentService.save(resident);
            redirectAttributes.addFlashAttribute("success", creating ? "住户已新增。" : "住户已更新。");
            return RedirectUrls.MANAGEMENT_RESIDENTS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return creating ? "redirect:/admin/residents/new" : "redirect:/admin/residents/edit/" + resident.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            residentService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "住户已删除。");
            return RedirectUrls.MANAGEMENT_RESIDENTS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/residents/edit/" + id;
        }
    }

    @GetMapping("/export")
    public void exportExcel(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String status,
                            HttpServletResponse response) throws IOException {
        var list = residentService.listAll(keyword, status);
        String[] headers = {"姓名", "房号", "手机号", "证件号", "住户类型", "状态"};
        ExcelExportHelper.export(response, "住户列表", "住户列表", headers, list, (row, r) -> {
            row.getCell(0).setCellValue(r.getName() != null ? r.getName() : "");
            row.getCell(1).setCellValue(r.getUnitNo() != null ? r.getUnitNo() : "");
            row.getCell(2).setCellValue(r.getPhone() != null ? r.getPhone() : "");
            row.getCell(3).setCellValue(r.getIdentityNo() != null ? r.getIdentityNo() : "");
            row.getCell(4).setCellValue(residentTypeLabel(r.getResidentType()));
            row.getCell(5).setCellValue("ACTIVE".equals(r.getStatus()) ? "在住" : "已迁出");
        });
    }

    private String residentTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "OWNER" -> "业主";
            case "TENANT" -> "租户";
            case "FAMILY" -> "家属";
            default -> type;
        };
    }
}

