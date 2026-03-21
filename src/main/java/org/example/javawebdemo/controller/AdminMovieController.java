package org.example.javawebdemo.controller;

import java.util.List;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.service.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {
    private static final String EDIT_DISABLED_MSG = "影片信息维护功能已禁用，请仅进行排片管理。";

    private final MovieService movieService;

    public AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public String list(Model model) {
        List<Movie> movies = movieService.listAll();
        model.addAttribute("movies", movies);
        return "admin/movies";
    }

    @GetMapping("/new")
    public String createForm(RedirectAttributes redirectAttributes) {
        return redirectDisabled(redirectAttributes);
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return redirectDisabled(redirectAttributes);
    }

    @PostMapping("/save")
    public String save(RedirectAttributes redirectAttributes) {
        return redirectDisabled(redirectAttributes);
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return redirectDisabled(redirectAttributes);
    }

    private String redirectDisabled(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", EDIT_DISABLED_MSG);
        return "redirect:/admin/movies";
    }
}
