package org.example.javawebdemo.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.MovieStatus;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.ShowService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {
    private final MovieService movieService;
    private final ShowService showService;
    private final HallService hallService;

    public HomeController(MovieService movieService, ShowService showService, HallService hallService) {
        this.movieService = movieService;
        this.showService = showService;
        this.hallService = hallService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Movie> movies = movieService.listByStatus(MovieStatus.ONLINE);
        model.addAttribute("movies", movies);
        return "index";
    }

    @GetMapping("/movies/{id}")
    public String movieDetail(@PathVariable Long id, Model model) {
        Movie movie = movieService.getById(id);
        List<Show> shows = showService.listWithFilters(LocalDate.now(), id, null);
        Map<Long, Hall> halls = new HashMap<>();
        for (Show show : shows) {
            halls.put(show.getHallId(), hallService.getById(show.getHallId()));
        }
        model.addAttribute("movie", movie);
        model.addAttribute("shows", shows);
        model.addAttribute("halls", halls);
        return "movies/detail";
    }
}
