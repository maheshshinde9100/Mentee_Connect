package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.model.Notification;
import java.util.List;

public interface NotificationService {
    Notification sendNotification(Notification notification);
    List<Notification> getNotificationsByRecipientType(String recipientType);
    List<Notification> getNotificationsByRecipientId(String recipientId);
    Notification markAsRead(String notificationId);
    void deleteNotification(String notificationId);
    List<Notification> getUnreadNotifications(String recipientId);
    List<Notification> getNotificationsForUser(String userId, String roleType);
    List<Notification> getUnreadForUser(String userId, String roleType);
    long countUnreadForUser(String userId, String roleType);
    int markAllAsReadForUser(String userId, String roleType);
}
