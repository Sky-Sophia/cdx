package org.example.javawebdemo.controller;

import java.time.LocalDate;
import java.util.List;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.MovieStatus;
import org.example.javawebdemo.service.FileStorageService;
import org.example.javawebdemo.service.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {
    private final MovieService movieService;
    private final FileStorageService fileStorageService;
    private final org.example.javawebdemo.service.ShowService showService;

    public AdminMovieController(MovieService movieService,
                                FileStorageService fileStorageService,
                                org.example.javawebdemo.service.ShowService showService) {
        this.movieService = movieService;
        this.fileStorageService = fileStorageService;
        this.showService = showService;
    }

    @GetMapping
    public String list(Model model) {
        List<Movie> movies = movieService.listAll();
        model.addAttribute("movies", movies);
        return "admin/movies";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movie", new Movie());
        model.addAttribute("statuses", MovieStatus.values());
        return "admin/movie-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Movie movie = movieService.getById(id);
        model.addAttribute("movie", movie);
        model.addAttribute("statuses", MovieStatus.values());
        return "admin/movie-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String title,
                       @RequestParam(required = false) String director,
                       @RequestParam(required = false) String actors,
                       @RequestParam(required = false) String genre,
                       @RequestParam(required = false) Integer durationMinutes,
                       @RequestParam(required = false) LocalDate releaseDate,
                       @RequestParam(required = false) String synopsis,
                       @RequestParam MovieStatus status,
                       @RequestParam(required = false) MultipartFile poster,
                       RedirectAttributes redirectAttributes) {
        Movie movie = id == null ? new Movie() : movieService.getById(id);
        movie.setTitle(title);
        movie.setDirector(director);
        movie.setActors(actors);
        movie.setGenre(genre);
        movie.setDurationMinutes(durationMinutes);
        movie.setReleaseDate(releaseDate);
        movie.setSynopsis(synopsis);
        movie.setStatus(status);
        if (poster != null && !poster.isEmpty()) {
            String url = fileStorageService.store(poster);
            movie.setPosterUrl(url);
        }
        if (id == null) {
            movieService.create(movie);
        } else {
            movieService.update(movie);
        }
        redirectAttributes.addFlashAttribute("success", "淇濆瓨鎴愬姛");
        return "redirect:/admin/movies";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (showService.hasShowsForMovie(id)) {
            redirectAttributes.addFlashAttribute("error", "该影片存在排片记录，无法删除。请先处理排片。");
            return "redirect:/admin/movies";
        }
        movieService.delete(id);
        redirectAttributes.addFlashAttribute("success", "鍒犻櫎鎴愬姛");
        return "redirect:/admin/movies";
    }
}

