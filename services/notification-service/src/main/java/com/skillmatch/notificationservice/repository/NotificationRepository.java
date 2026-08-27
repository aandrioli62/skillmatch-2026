package com.skillmatch.notificationservice.repository;

import com.skillmatch.notificationservice.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
}
