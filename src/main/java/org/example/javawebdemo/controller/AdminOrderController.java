package org.example.javawebdemo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderStatus;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.model.User;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.OrderService;
import org.example.javawebdemo.service.ShowService;
import org.example.javawebdemo.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {
    private final OrderService orderService;
    private final UserService userService;
    private final ShowService showService;
    private final MovieService movieService;
    private final HallService hallService;

    public AdminOrderController(OrderService orderService,
                                UserService userService,
                                ShowService showService,
                                MovieService movieService,
                                HallService hallService) {
        this.orderService = orderService;
        this.userService = userService;
        this.showService = showService;
        this.movieService = movieService;
        this.hallService = hallService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long userId,
                       @RequestParam(required = false) OrderStatus status,
                       Model model) {
        List<Order> orders = orderService.listAll(userId, status);
        Map<Long, User> users = new HashMap<>();
        Map<Long, Show> shows = new HashMap<>();
        Map<Long, Movie> movies = new HashMap<>();
        Map<Long, Hall> halls = new HashMap<>();
        for (Order order : orders) {
            User user = userService.findById(order.getUserId());
            users.put(user.getId(), user);
            Show show = showService.getById(order.getShowId());
            shows.put(show.getId(), show);
            Movie movie = movieService.getById(show.getMovieId());
            movies.put(movie.getId(), movie);
            Hall hall = hallService.getById(show.getHallId());
            halls.put(hall.getId(), hall);
        }
        model.addAttribute("orders", orders);
        model.addAttribute("users", users);
        model.addAttribute("shows", shows);
        model.addAttribute("movies", movies);
        model.addAttribute("halls", halls);
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("userId", userId);
        model.addAttribute("status", status);
        return "admin/orders";
    }
}
