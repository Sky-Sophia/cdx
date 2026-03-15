package org.example.javawebdemo.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.ShowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/staff/shows")
public class StaffShowController {
    private final ShowService showService;
    private final MovieService movieService;
    private final HallService hallService;

    public StaffShowController(ShowService showService, MovieService movieService, HallService hallService) {
        this.showService = showService;
        this.movieService = movieService;
        this.hallService = hallService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                       @RequestParam(required = false) Long movieId,
                       @RequestParam(required = false) Long hallId,
                       Model model) {
        List<Show> shows = showService.listWithFilters(date, movieId, hallId);
        List<Movie> movies = movieService.listAll();
        List<Hall> halls = hallService.listAll();
        Map<Long, Movie> movieMap = new HashMap<>();
        for (Movie movie : movies) {
            movieMap.put(movie.getId(), movie);
        }
        Map<Long, Hall> hallMap = new HashMap<>();
        for (Hall hall : halls) {
            hallMap.put(hall.getId(), hall);
        }
        model.addAttribute("shows", shows);
        model.addAttribute("movies", movies);
        model.addAttribute("halls", halls);
        model.addAttribute("movieMap", movieMap);
        model.addAttribute("hallMap", hallMap);
        return "staff/shows";
    }
}
