package org.example.propertyms.notification.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @DeleteMapping("/{id}")
    public DeleteOneResponse deleteOne(@PathVariable("id") Long id, HttpSession session) {
        UserSession currentUser = requireCurrentUser(session);
        Long deletedId = notificationService.delete(currentUser.getId(), id);
        int unreadCount = notificationService.countUnread(currentUser.getId());
        return new DeleteOneResponse(deletedId, unreadCount);
    }

    @DeleteMapping
    public DeleteAllResponse deleteAll(HttpSession session) {
        UserSession currentUser = requireCurrentUser(session);
        List<Long> ids = notificationService.deleteAll(currentUser.getId());
        int unreadCount = notificationService.countUnread(currentUser.getId());
        return new DeleteAllResponse(ids, unreadCount, ids.size());
    }

    private UserSession requireCurrentUser(HttpSession session) {
        UserSession currentUser = (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前登录状态无效，请重新登录。");
        }
        return currentUser;
    }

    public record DeleteOneResponse(Long id, int unreadCount) {
    }

    public record DeleteAllResponse(List<Long> ids, int unreadCount, int deletedCount) {
    }
}

