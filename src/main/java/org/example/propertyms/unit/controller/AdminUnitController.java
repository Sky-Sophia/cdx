package org.example.propertyms.unit.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.example.propertyms.building.service.BuildingService;
import org.example.propertyms.common.constant.RedirectUrls;
import org.example.propertyms.common.util.ExcelExportHelper;
import org.example.propertyms.unit.model.PropertyUnit;
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
@RequestMapping("/admin/units")
public class AdminUnitController {
    private final PropertyUnitService propertyUnitService;
    private final BuildingService buildingService;

    public AdminUnitController(PropertyUnitService propertyUnitService, BuildingService buildingService) {
        this.propertyUnitService = propertyUnitService;
        this.buildingService = buildingService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Long buildingId,
                       @RequestParam(required = false) String status) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/management")
                .queryParam("tab", "units");
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("unitKeyword", keyword);
        }
        if (buildingId != null) {
            builder.queryParam("unitBuildingId", buildingId);
        }
        if (status != null && !status.isBlank()) {
            builder.queryParam("unitStatus", status);
        }
        return "redirect:" + builder.toUriString();
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("unit", new PropertyUnit());
        model.addAttribute("buildings", buildingService.listAll());
        model.addAttribute("editing", false);
        return "admin/units/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        PropertyUnit unit = propertyUnitService.findById(id);
        if (unit == null) {
            redirectAttributes.addFlashAttribute("error", "房屋不存在。");
            return RedirectUrls.MANAGEMENT_UNITS;
        }
        model.addAttribute("unit", unit);
        model.addAttribute("buildings", buildingService.listAll());
        model.addAttribute("editing", true);
        return "admin/units/form";
    }

    @PostMapping("/save")
    public String save(PropertyUnit unit, RedirectAttributes redirectAttributes) {
        boolean creating = unit.getId() == null;
        try {
            propertyUnitService.save(unit);
            redirectAttributes.addFlashAttribute("success", creating ? "房屋已新增。" : "房屋已更新。");
            return RedirectUrls.MANAGEMENT_UNITS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return creating ? "redirect:/admin/units/new" : "redirect:/admin/units/edit/" + unit.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            propertyUnitService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "房屋已删除。");
            return RedirectUrls.MANAGEMENT_UNITS;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/units/edit/" + id;
        }
    }

    @GetMapping("/export")
    public void exportExcel(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long buildingId,
                            @RequestParam(required = false) String status,
                            HttpServletResponse response) throws IOException {
        var list = propertyUnitService.listAll(keyword, buildingId, status);
        String[] headers = {"楼栋", "房号", "楼层", "建筑面积(m²)", "业主", "手机号", "状态"};
        ExcelExportHelper.export(response, "房屋列表", "房屋列表", headers, list, (row, u) -> {
            row.getCell(0).setCellValue(u.getBuildingName() != null ? u.getBuildingName() : "");
            row.getCell(1).setCellValue(u.getUnitNo() != null ? u.getUnitNo() : "");
            row.getCell(2).setCellValue(u.getFloorNo() != null ? u.getFloorNo() : 0);
            row.getCell(3).setCellValue(u.getAreaM2() != null ? u.getAreaM2().doubleValue() : 0);
            row.getCell(4).setCellValue(u.getOwnerName() != null ? u.getOwnerName() : "");
            row.getCell(5).setCellValue(u.getOwnerPhone() != null ? u.getOwnerPhone() : "");
            row.getCell(6).setCellValue(statusLabel(u.getOccupancyStatus()));
        });
    }

    private String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "OCCUPIED" -> "自住";
            case "RENTED" -> "出租";
            case "VACANT" -> "空置";
            default -> status;
        };
    }
}

