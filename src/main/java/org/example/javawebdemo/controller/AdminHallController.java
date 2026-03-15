package org.example.javawebdemo.controller;

import java.util.List;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.HallType;
import org.example.javawebdemo.model.SeatLayout;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.util.SeatLayoutUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/halls")
public class AdminHallController {
    private final HallService hallService;
    private final org.example.javawebdemo.service.ShowService showService;
    private final MessageSource messageSource;

    public AdminHallController(HallService hallService,
                               org.example.javawebdemo.service.ShowService showService,
                               MessageSource messageSource) {
        this.hallService = hallService;
        this.showService = showService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String list(Model model) {
        List<Hall> halls = hallService.listAll();
        model.addAttribute("halls", halls);
        return "admin/halls";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Hall hall = new Hall();
        SeatLayout layout = SeatLayoutUtils.parse(hall.getSeatLayoutJson());
        int rows = layout != null && layout.getRows() != null ? layout.getRows() : 8;
        int cols = layout != null && layout.getCols() != null ? layout.getCols() : 12;
        String disabledSeats = layout != null && layout.getDisabled() != null
                ? String.join(",", layout.getDisabled())
                : "";
        model.addAttribute("hall", hall);
        model.addAttribute("types", HallType.values());
        model.addAttribute("rows", rows);
        model.addAttribute("cols", cols);
        model.addAttribute("disabledSeats", disabledSeats);
        return "admin/hall-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Hall hall = hallService.getById(id);
        SeatLayout layout = SeatLayoutUtils.parse(hall == null ? null : hall.getSeatLayoutJson());
        int rows = layout != null && layout.getRows() != null ? layout.getRows() : 8;
        int cols = layout != null && layout.getCols() != null ? layout.getCols() : 12;
        String disabledSeats = layout != null && layout.getDisabled() != null
                ? String.join(",", layout.getDisabled())
                : "";
        model.addAttribute("hall", hall);
        model.addAttribute("types", HallType.values());
        model.addAttribute("rows", rows);
        model.addAttribute("cols", cols);
        model.addAttribute("disabledSeats", disabledSeats);
        return "admin/hall-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam HallType hallType,
                       @RequestParam int rows,
                       @RequestParam int cols,
                       @RequestParam(required = false) String disabledSeats,
                       RedirectAttributes redirectAttributes) {
        String layoutJson = SeatLayoutUtils.buildJson(rows, cols, disabledSeats);
        int disabledCount = 0;
        if (disabledSeats != null && !disabledSeats.isBlank()) {
            disabledCount = disabledSeats.split(",").length;
        }
        int seatTotal = rows * cols - disabledCount;
        Hall hall = id == null ? new Hall() : hallService.getById(id);
        hall.setName(name);
        hall.setHallType(hallType);
        hall.setSeatTotal(seatTotal);
        hall.setSeatLayoutJson(layoutJson);
        if (id == null) {
            hallService.create(hall);
        } else {
            hallService.update(hall);
        }
        redirectAttributes.addFlashAttribute("success", msg("admin.hall.save.success"));
        return "redirect:/admin/halls";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (showService.hasShowsForHall(id)) {
            redirectAttributes.addFlashAttribute("error", msg("admin.hall.delete.blocked"));
            return "redirect:/admin/halls";
        }
        hallService.delete(id);
        redirectAttributes.addFlashAttribute("success", msg("admin.hall.delete.success"));
        return "redirect:/admin/halls";
    }

    private String msg(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}

