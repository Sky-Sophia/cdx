package org.example.javawebdemo.controller;

import org.example.javawebdemo.model.PropertyUnit;
import org.example.javawebdemo.service.BuildingService;
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
        return "admin/unit-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        PropertyUnit unit = propertyUnitService.findById(id);
        if (unit == null) {
            redirectAttributes.addFlashAttribute("error", "房屋不存在。");
            return "redirect:/admin/management?tab=units";
        }
        model.addAttribute("unit", unit);
        model.addAttribute("buildings", buildingService.listAll());
        model.addAttribute("editing", true);
        return "admin/unit-form";
    }

    @PostMapping("/save")
    public String save(PropertyUnit unit, RedirectAttributes redirectAttributes) {
        boolean creating = unit.getId() == null;
        try {
            propertyUnitService.save(unit);
            redirectAttributes.addFlashAttribute("success", creating ? "房屋已新增。" : "房屋已更新。");
            return "redirect:/admin/management?tab=units";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return creating ? "redirect:/admin/units/new" : "redirect:/admin/units/edit/" + unit.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        propertyUnitService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "房屋已删除。");
        return "redirect:/admin/management?tab=units";
    }
}
