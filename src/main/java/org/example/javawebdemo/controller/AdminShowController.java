package org.example.javawebdemo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.HallType;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.model.ShowStatus;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.SeatService;
import org.example.javawebdemo.service.ShowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/shows")
public class AdminShowController {
    private final ShowService showService;
    private final MovieService movieService;
    private final HallService hallService;
    private final SeatService seatService;

    public AdminShowController(ShowService showService, MovieService movieService,
                               HallService hallService, SeatService seatService) {
        this.showService = showService;
        this.movieService = movieService;
        this.hallService = hallService;
        this.seatService = seatService;
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
        return "admin/shows";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("movies", movieService.listAll());
        model.addAttribute("halls", hallService.listAll());
        return "admin/show-form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long movieId,
                       @RequestParam Long hallId,
                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime,
                       @RequestParam BigDecimal basePrice,
                       RedirectAttributes redirectAttributes) {
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            redirectAttributes.addFlashAttribute("error", "结束时间必须晚于开始时间");
            return "redirect:/admin/shows/new";
        }
        if (showService.hasConflict(hallId, startTime, endTime, null)) {
            redirectAttributes.addFlashAttribute("error", "排片时间冲突");
            return "redirect:/admin/shows/new";
        }
        Hall hall = hallService.getById(hallId);
        BigDecimal finalPrice = basePrice;
        if (hall.getHallType() == HallType.IMAX) {
            finalPrice = basePrice.multiply(new BigDecimal("1.5"));
        }
        Show show = new Show();
        show.setMovieId(movieId);
        show.setHallId(hallId);
        show.setStartTime(startTime);
        show.setEndTime(endTime);
        show.setBasePrice(basePrice);
        show.setFinalPrice(finalPrice);
        show.setStatus(ShowStatus.SCHEDULED);
        showService.create(show);
        seatService.generateSeatsForShow(show);
        redirectAttributes.addFlashAttribute("success", "排片创建成功");
        return "redirect:/admin/shows";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Show show = showService.getById(id);
        if (show != null) {
            show.setStatus(ShowStatus.CANCELED);
            showService.update(show);
        }
        redirectAttributes.addFlashAttribute("success", "已取消排片");
        return "redirect:/admin/shows";
    }
}
