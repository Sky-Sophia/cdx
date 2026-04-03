package org.example.javawebdemo.controller;

import org.example.javawebdemo.model.Resident;
import org.example.javawebdemo.service.PropertyUnitService;
import org.example.javawebdemo.service.ResidentService;
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
        return "admin/resident-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Resident resident = residentService.findById(id);
        if (resident == null) {
            redirectAttributes.addFlashAttribute("error", "住户不存在。");
            return "redirect:/admin/management?tab=residents";
        }
        model.addAttribute("resident", resident);
        model.addAttribute("units", propertyUnitService.listSimple());
        model.addAttribute("editing", true);
        return "admin/resident-form";
    }

    @PostMapping("/save")
    public String save(Resident resident, RedirectAttributes redirectAttributes) {
        boolean creating = resident.getId() == null;
        try {
            residentService.save(resident);
            redirectAttributes.addFlashAttribute("success", creating ? "住户已新增。" : "住户已更新。");
            return "redirect:/admin/management?tab=residents";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return creating ? "redirect:/admin/residents/new" : "redirect:/admin/residents/edit/" + resident.getId();
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        residentService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "住户已删除。");
        return "redirect:/admin/management?tab=residents";
    }
}
