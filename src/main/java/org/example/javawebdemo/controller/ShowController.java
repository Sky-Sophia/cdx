package org.example.javawebdemo.controller;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.dto.SeatRowView;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.SeatService;
import org.example.javawebdemo.service.ShowService;
import org.example.javawebdemo.service.UserService;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/shows")
public class ShowController {
    private final ShowService showService;
    private final MovieService movieService;
    private final HallService hallService;
    private final SeatService seatService;
    private final UserService userService;

    @Value("${app.seat-lock-minutes:15}")
    private int seatLockMinutes;

    public ShowController(ShowService showService,
                          MovieService movieService,
                          HallService hallService,
                          SeatService seatService,
                          UserService userService) {
        this.showService = showService;
        this.movieService = movieService;
        this.hallService = hallService;
        this.seatService = seatService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                       @RequestParam(required = false) Long movieId,
                       @RequestParam(required = false) Long hallId,
                       Model model) {
        LocalDate queryDate = date == null ? LocalDate.now() : date;
        List<Show> shows = showService.listWithFilters(queryDate, movieId, hallId);
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
        model.addAttribute("date", queryDate);
        model.addAttribute("movieId", movieId);
        model.addAttribute("hallId", hallId);
        return "shows/list";
    }

    @GetMapping("/{id}")
    public String showDetail(@PathVariable Long id, Model model, HttpSession session) {
        Show show = showService.getById(id);
        if (show == null) {
            model.addAttribute("error", "场次不存在");
            return "shows/detail";
        }
        if (show.getStatus() != null && show.getStatus() != org.example.javawebdemo.model.ShowStatus.SCHEDULED) {
            model.addAttribute("error", "场次已取消或结束");
            return "shows/detail";
        }
        if (show.getStartTime() != null && show.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            model.addAttribute("error", "场次已开始，无法选座");
            return "shows/detail";
        }
        Movie movie = movieService.getById(show.getMovieId());
        Hall hall = hallService.getById(show.getHallId());
        if (movie == null || hall == null) {
            model.addAttribute("error", "场次对应的影片或影厅已删除");
            model.addAttribute("show", null);
            return "shows/detail";
        }
        List<SeatRowView> seatRows;
        try {
            seatRows = seatService.buildSeatMap(id);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "shows/detail";
        }
        model.addAttribute("show", show);
        model.addAttribute("movie", movie);
        model.addAttribute("hall", hall);
        model.addAttribute("seatRows", seatRows);
        model.addAttribute("seatLockMinutes", seatLockMinutes);
        model.addAttribute("currentUser", session.getAttribute(SessionKeys.CURRENT_USER));
        return "shows/detail";
    }

    @PostMapping("/{id}/lock")
    public String lockSeats(@PathVariable Long id,
                            @RequestParam(value = "seatIds", required = false) List<Long> seatIds,
                            @RequestParam(value = "buyerUsername", required = false) String buyerUsername,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        UserSession currentUser = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/login";
        }
        Long userId = currentUser.getId();
        boolean canProxy = currentUser.getRole().name().equals("ADMIN") || currentUser.getRole().name().equals("STAFF");
        if (canProxy && buyerUsername != null && !buyerUsername.isBlank()) {
            User buyer = userService.findByUsername(buyerUsername.trim());
            if (buyer == null) {
                redirectAttributes.addFlashAttribute("error", "购票用户不存在");
                return "redirect:/shows/" + id;
            }
            userId = buyer.getId();
        }
        try {
            Order order = seatService.lockSeatsAndCreateOrder(id, seatIds, userId);
            return "redirect:/orders/" + order.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/shows/" + id;
        }
    }
}

