package org.example.javawebdemo.controller;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.javawebdemo.dto.UserSession;
import org.example.javawebdemo.mapper.OrderItemMapper;
import org.example.javawebdemo.model.Hall;
import org.example.javawebdemo.model.Movie;
import org.example.javawebdemo.model.Order;
import org.example.javawebdemo.model.OrderItem;
import org.example.javawebdemo.model.Role;
import org.example.javawebdemo.model.Show;
import org.example.javawebdemo.service.HallService;
import org.example.javawebdemo.service.MovieService;
import org.example.javawebdemo.service.OrderService;
import org.example.javawebdemo.service.SeatService;
import org.example.javawebdemo.service.ShowService;
import org.example.javawebdemo.util.SessionKeys;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {
    private final OrderService orderService;
    private final OrderItemMapper orderItemMapper;
    private final SeatService seatService;
    private final ShowService showService;
    private final MovieService movieService;
    private final HallService hallService;

    public OrderController(OrderService orderService,
                           OrderItemMapper orderItemMapper,
                           SeatService seatService,
                           ShowService showService,
                           MovieService movieService,
                           HallService hallService) {
        this.orderService = orderService;
        this.orderItemMapper = orderItemMapper;
        this.seatService = seatService;
        this.showService = showService;
        this.movieService = movieService;
        this.hallService = hallService;
    }

    @GetMapping("/orders")
    public String myOrders(HttpSession session, Model model) {
        UserSession user = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        List<Order> orders = orderService.listByUser(user.getId());
        Map<Long, Show> shows = new HashMap<>();
        Map<Long, Movie> movies = new HashMap<>();
        Map<Long, Hall> halls = new HashMap<>();
        for (Order order : orders) {
            Show show = showService.getById(order.getShowId());
            shows.put(show.getId(), show);
            movies.put(show.getMovieId(), movieService.getById(show.getMovieId()));
            halls.put(show.getHallId(), hallService.getById(show.getHallId()));
        }
        model.addAttribute("orders", orders);
        model.addAttribute("shows", shows);
        model.addAttribute("movies", movies);
        model.addAttribute("halls", halls);
        return "orders/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, HttpSession session, Model model) {
        UserSession user = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        Order order = orderService.getById(id);
        if (order == null) {
            model.addAttribute("error", "订单不存在");
            return "orders/detail";
        }
        if (!order.getUserId().equals(user.getId()) && user.getRole() == Role.USER) {
            model.addAttribute("error", "无权限查看该订单");
            return "orders/detail";
        }
        List<OrderItem> items = orderItemMapper.findByOrderId(id);
        Show show = showService.getById(order.getShowId());
        Movie movie = movieService.getById(show.getMovieId());
        Hall hall = hallService.getById(show.getHallId());
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("show", show);
        model.addAttribute("movie", movie);
        model.addAttribute("hall", hall);
        model.addAttribute("currentUser", user);
        return "orders/detail";
    }

    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        UserSession user = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        boolean allowAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        try {
            seatService.payOrder(id, user.getId(), allowAdmin);
            redirectAttributes.addFlashAttribute("success", "支付成功");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/refund")
    public String refund(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        UserSession user = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        boolean allowAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
        try {
            seatService.refundOrder(id, user.getId(), allowAdmin);
            redirectAttributes.addFlashAttribute("success", "退票成功");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
