package com.mahesh.mentee_connect.service.impl;

import com.mahesh.mentee_connect.model.Notification;
import com.mahesh.mentee_connect.repository.NotificationRepository;
import com.mahesh.mentee_connect.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Notification sendNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByRecipientType(String recipientType) {
        return notificationRepository.findByRecipientType(recipientType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByRecipientId(String recipientId) {
        // Get notifications specifically for this user
        List<Notification> userNotifications = notificationRepository.findByRecipientId(recipientId);
        
        // Get notifications for ALL recipients (broadcasts)
        List<Notification> broadcastNotifications = notificationRepository.findByRecipientType("ALL");
        
        // Get notifications for the user's role if available
        List<Notification> roleNotifications = new ArrayList<>();
        
        // Try to get the user type from the username/email pattern
        if (recipientId.contains("student")) {
            roleNotifications = notificationRepository.findByRecipientType("STUDENT");
        } else if (recipientId.contains("mentor")) {
            roleNotifications = notificationRepository.findByRecipientType("MENTOR");
        } else if (recipientId.contains("admin")) {
            roleNotifications = notificationRepository.findByRecipientType("ADMIN");
        }
        
        // Combine all notifications
        List<Notification> allNotifications = new ArrayList<>();
        allNotifications.addAll(userNotifications);
        allNotifications.addAll(broadcastNotifications);
        allNotifications.addAll(roleNotifications);
        
        return allNotifications;
    }

    @Override
    @Transactional
    public Notification markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException("Notification not found");
        }
        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String recipientId) {
        // Get unread notifications specifically for this user
        List<Notification> userUnread = notificationRepository.findByRecipientIdAndIsReadFalse(recipientId);
        
        // Get broadcast unread notifications
        List<Notification> broadcastUnread = notificationRepository.findByRecipientTypeAndIsReadFalse("ALL");
        
        // Get unread notifications for the user's role if available
        List<Notification> roleUnread = new ArrayList<>();
        
        // Try to get the user type from the username/email pattern
        if (recipientId.contains("student")) {
            roleUnread = notificationRepository.findByRecipientTypeAndIsReadFalse("STUDENT");
        } else if (recipientId.contains("mentor")) {
            roleUnread = notificationRepository.findByRecipientTypeAndIsReadFalse("MENTOR");
        } else if (recipientId.contains("admin")) {
            roleUnread = notificationRepository.findByRecipientTypeAndIsReadFalse("ADMIN");
        }
        
        // Combine them
        List<Notification> allUnread = new ArrayList<>();
        allUnread.addAll(userUnread);
        allUnread.addAll(broadcastUnread);
        allUnread.addAll(roleUnread);
        
        return allUnread;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(String userId, String roleType) {
        Map<String, Notification> unique = new LinkedHashMap<>();
        addAll(unique, notificationRepository.findByRecipientId(userId));
        addAll(unique, notificationRepository.findByRecipientType("ALL"));
        if (roleType != null && !roleType.isBlank()) {
            addAll(unique, notificationRepository.findByRecipientType(roleType));
        }

        List<Notification> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(
                Notification::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadForUser(String userId, String roleType) {
        Map<String, Notification> unique = new LinkedHashMap<>();
        addAll(unique, notificationRepository.findByRecipientIdAndIsReadFalse(userId));
        addAll(unique, notificationRepository.findByRecipientTypeAndIsReadFalse("ALL"));
        if (roleType != null && !roleType.isBlank()) {
            addAll(unique, notificationRepository.findByRecipientTypeAndIsReadFalse(roleType));
        }
        return new ArrayList<>(unique.values());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadForUser(String userId, String roleType) {
        return getUnreadForUser(userId, roleType).size();
    }

    @Override
    @Transactional
    public int markAllAsReadForUser(String userId, String roleType) {
        List<Notification> unread = getUnreadForUser(userId, roleType);
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    private void addAll(Map<String, Notification> target, List<Notification> source) {
        if (source == null) {
            return;
        }
        for (Notification notification : source) {
            if (notification == null) {
                continue;
            }
            String key = notification.getId() != null
                    ? notification.getId()
                    : notification.getTitle() + "|" + notification.getCreatedAt();
            target.putIfAbsent(key, notification);
        }
    }
} 