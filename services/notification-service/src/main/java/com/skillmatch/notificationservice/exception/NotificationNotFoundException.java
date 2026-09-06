package com.skillmatch.notificationservice.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String notificationId) {
        super("Notification not found: id=" + notificationId);
    }
}
