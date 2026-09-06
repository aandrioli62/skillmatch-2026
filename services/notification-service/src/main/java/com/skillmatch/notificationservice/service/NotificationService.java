package com.skillmatch.notificationservice.service;

import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.event.IncomingEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    /**
     * Turns a domain event into one notification per relevant recipient (e.g.
     * candidature.accepted notifies both the professional and the company), persists
     * each, and "sends" it — for now, mocking the email channel by logging it (Fase 5).
     * Unrecognized event types still produce a generic, unaddressed notification so
     * nothing silently disappears.
     */
    void processEvent(IncomingEvent event);

    /**
     * Returns all notifications addressed to the given user, most recent first.
     */
    List<NotificationResponse> listMine(UUID recipientId);

    /**
     * Marks the given notification as read, on behalf of the given recipient.
     * Throws if the notification does not exist, or does not belong to the recipient.
     */
    NotificationResponse markAsRead(String notificationId, UUID recipientId);
}
