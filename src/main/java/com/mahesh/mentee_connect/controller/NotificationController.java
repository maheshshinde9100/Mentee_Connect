package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.Notification;
import com.mahesh.mentee_connect.model.User;
import com.mahesh.mentee_connect.dto.NotificationRequest;
import com.mahesh.mentee_connect.service.AuthService;
import com.mahesh.mentee_connect.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthService authService;

    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendNotification(@Valid @RequestBody NotificationRequest request) {
        User sender = authService.getCurrentUser();
        if (sender == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String senderName = ((sender.getFirstName() == null ? "" : sender.getFirstName()) + " "
                + (sender.getLastName() == null ? "" : sender.getLastName())).trim();

        Notification notification = new Notification(
                request.getTitle(),
                request.getMessage(),
                sender.getId(),
                senderName.isEmpty() ? sender.getEmail() : senderName,
                request.getRecipientType());

        if (request.getRecipientId() != null) {
            notification.setRecipientId(request.getRecipientId());
        }

        return ResponseEntity.ok(notificationService.sendNotification(notification));
    }

    @GetMapping("/my-notifications")
    public ResponseEntity<List<Notification>> getMyNotifications() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getNotificationsForUser(user.getId(), roleType(user)));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.getUnreadForUser(user.getId(), roleType(user)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of(
                "count", notificationService.countUnreadForUser(user.getId(), roleType(user))));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        User user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(notificationService.markAsRead(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        User user = authService.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int updated = notificationService.markAllAsReadForUser(user.getId(), roleType(user));
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    private String roleType(User user) {
        return user.getRole() == null ? null : user.getRole().name().replace("ROLE_", "");
    }
}
