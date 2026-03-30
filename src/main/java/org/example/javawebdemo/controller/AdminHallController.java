package org.example.javawebdemo.controller;

import org.example.javawebdemo.mapper.BuildingMapper;
import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.model.PropertyUnit;
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
public class AdminHallController {
    private final PropertyUnitMapper propertyUnitMapper;
    private final BuildingMapper buildingMapper;

    public AdminHallController(PropertyUnitMapper propertyUnitMapper, BuildingMapper buildingMapper) {
        this.propertyUnitMapper = propertyUnitMapper;
        this.buildingMapper = buildingMapper;
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
        model.addAttribute("buildings", buildingMapper.findAll());
        model.addAttribute("editing", false);
        return "admin/unit-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        PropertyUnit unit = propertyUnitMapper.findById(id);
        if (unit == null) {
            redirectAttributes.addFlashAttribute("error", "房屋不存在。");
            return "redirect:/admin/management?tab=units";
        }
        model.addAttribute("unit", unit);
        model.addAttribute("buildings", buildingMapper.findAll());
        model.addAttribute("editing", true);
        return "admin/unit-form";
    }

    @PostMapping("/save")
    public String save(PropertyUnit unit, RedirectAttributes redirectAttributes) {
        if (unit.getBuildingId() == null || unit.getUnitNo() == null || unit.getUnitNo().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "请填写楼栋和房号。");
            return unit.getId() == null ? "redirect:/admin/units/new" : "redirect:/admin/units/edit/" + unit.getId();
        }

        if (unit.getOccupancyStatus() == null || unit.getOccupancyStatus().isBlank()) {
            unit.setOccupancyStatus("VACANT");
        }

        if (unit.getId() == null) {
            propertyUnitMapper.insert(unit);
            redirectAttributes.addFlashAttribute("success", "房屋已新增。");
        } else {
            propertyUnitMapper.update(unit);
            redirectAttributes.addFlashAttribute("success", "房屋已更新。");
        }
        return "redirect:/admin/management?tab=units";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        propertyUnitMapper.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "房屋已删除。");
        return "redirect:/admin/management?tab=units";
    }
}
