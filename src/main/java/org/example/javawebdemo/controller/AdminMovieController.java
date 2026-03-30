package org.example.javawebdemo.controller;

import org.example.javawebdemo.mapper.PropertyUnitMapper;
import org.example.javawebdemo.mapper.ResidentMapper;
import org.example.javawebdemo.model.Resident;
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
public class AdminMovieController {
    private final ResidentMapper residentMapper;
    private final PropertyUnitMapper propertyUnitMapper;

    public AdminMovieController(ResidentMapper residentMapper, PropertyUnitMapper propertyUnitMapper) {
        this.residentMapper = residentMapper;
        this.propertyUnitMapper = propertyUnitMapper;
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
        model.addAttribute("units", propertyUnitMapper.findAllSimple());
        model.addAttribute("editing", false);
        return "admin/resident-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Resident resident = residentMapper.findById(id);
        if (resident == null) {
            redirectAttributes.addFlashAttribute("error", "住户不存在。");
            return "redirect:/admin/management?tab=residents";
        }
        model.addAttribute("resident", resident);
        model.addAttribute("units", propertyUnitMapper.findAllSimple());
        model.addAttribute("editing", true);
        return "admin/resident-form";
    }

    @PostMapping("/save")
    public String save(Resident resident, RedirectAttributes redirectAttributes) {
        if (resident.getUnitId() == null || resident.getName() == null || resident.getName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "请填写房屋和住户姓名。");
            return resident.getId() == null ? "redirect:/admin/residents/new" : "redirect:/admin/residents/edit/" + resident.getId();
        }

        if (resident.getStatus() == null || resident.getStatus().isBlank()) {
            resident.setStatus("ACTIVE");
        }
        if (resident.getResidentType() == null || resident.getResidentType().isBlank()) {
            resident.setResidentType("OWNER");
        }

        if (resident.getId() == null) {
            residentMapper.insert(resident);
            redirectAttributes.addFlashAttribute("success", "住户已新增。");
        } else {
            residentMapper.update(resident);
            redirectAttributes.addFlashAttribute("success", "住户已更新。");
        }
        return "redirect:/admin/management?tab=residents";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        residentMapper.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "住户已删除。");
        return "redirect:/admin/management?tab=residents";
    }
}
