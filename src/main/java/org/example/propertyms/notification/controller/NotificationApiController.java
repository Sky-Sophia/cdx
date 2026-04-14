package org.example.propertyms.notification.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.constant.SessionKeys;
import org.example.propertyms.notification.model.NotificationItem;
import org.example.propertyms.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
        Long currentUserId = requireCurrentUserId(session);
        Long deletedId = notificationService.delete(currentUserId, id);
        return new DeleteOneResponse(deletedId, unreadCount(currentUserId));
    }

    @PatchMapping("/{id}/popup-hide")
    public HidePopupResponse hidePopup(@PathVariable("id") Long id, HttpSession session) {
        Long currentUserId = requireCurrentUserId(session);
        NotificationItem item = notificationService.hidePopup(currentUserId, id);
        return new HidePopupResponse(item, unreadCount(currentUserId));
    }

    @DeleteMapping
    public DeleteAllResponse deleteAll(HttpSession session) {
        Long currentUserId = requireCurrentUserId(session);
        List<Long> ids = notificationService.deleteAll(currentUserId);
        return new DeleteAllResponse(ids, unreadCount(currentUserId), ids.size());
    }

    private Long requireCurrentUserId(HttpSession session) {
        UserSession currentUser = session == null ? null : (UserSession) session.getAttribute(SessionKeys.CURRENT_USER);
        if (currentUser == null || currentUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "当前登录状态无效，请重新登录。");
        }
        return currentUser.getId();
    }

    private int unreadCount(Long currentUserId) {
        return notificationService.countUnread(currentUserId);
    }

    public record DeleteOneResponse(Long id, int unreadCount) {
    }

    public record HidePopupResponse(NotificationItem item, int unreadCount) {
    }

    public record DeleteAllResponse(List<Long> ids, int unreadCount, int deletedCount) {
    }
}
