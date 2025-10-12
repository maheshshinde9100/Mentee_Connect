package com.mahesh.mentee_connect.controller;

import com.mahesh.mentee_connect.model.Notification;
import com.mahesh.mentee_connect.dto.NotificationRequest;
import com.mahesh.mentee_connect.service.NotificationService;
import com.mahesh.mentee_connect.security.jwt.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping({"/notifications", "/api/notifications"})
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtils jwtUtils;

    @GetMapping("/auth-test")
    public ResponseEntity<?> testAuth(Authentication authentication) {
        if (authentication != null) {
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "authenticated", false
            ));
        }
    }

    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendNotification(
            @Valid @RequestBody NotificationRequest request,
            Authentication authentication) {
        
        Notification notification = new Notification(
            request.getTitle(),
            request.getMessage(),
            authentication.getName(),
            authentication.getName(),
            request.getRecipientType()
        );
        
        if (request.getRecipientId() != null) {
            notification.setRecipientId(request.getRecipientId());
        }
        
        Notification savedNotification = notificationService.sendNotification(notification);
        return ResponseEntity.ok(savedNotification);
    }

    @GetMapping("/my-notifications")
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String userId = authentication.getName();
        List<Notification> notifications = notificationService.getNotificationsByRecipientId(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String userId = authentication.getName();
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable String id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/auth-status")
    public ResponseEntity<?> showAuthStatus(Authentication authentication, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader != null) {
            response.put("auth_header_present", true);
            response.put("auth_header_starts_with_bearer", authHeader.startsWith("Bearer "));
            if (authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    response.put("token_length", token.length());
                    response.put("token_first_10_chars", token.substring(0, Math.min(10, token.length())));
                    // Get token information
                    try {
                        String username = jwtUtils.getUserNameFromJwtToken(token);
                        response.put("extracted_username", username);
                    } catch (Exception e) {
                        response.put("username_extraction_error", e.getMessage());
                    }
                } catch (Exception e) {
                    response.put("token_processing_error", e.getMessage());
                }
            }
        } else {
            response.put("auth_header_present", false);
        }
        
        if (authentication != null) {
            response.put("authentication_object_present", true);
            response.put("authentication_name", authentication.getName());
            response.put("authentication_principal", authentication.getPrincipal().toString());
            response.put("authentication_authorities", authentication.getAuthorities().toString());
            response.put("authentication_details", authentication.getDetails() != null ? authentication.getDetails().toString() : "null");
        } else {
            response.put("authentication_object_present", false);
        }
        
        return ResponseEntity.ok(response);
    }
} 