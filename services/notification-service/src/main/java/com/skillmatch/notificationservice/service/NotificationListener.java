package com.skillmatch.notificationservice.service;

import com.skillmatch.notificationservice.config.RabbitMQConfig;
import com.skillmatch.notificationservice.event.IncomingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ALL_EVENTS)
    public void onEvent(IncomingEvent event) {
        log.info("Received event: eventType={}, eventId={}", event.getEventType(), event.getEventId());
        notificationService.processEvent(event);
    }
}
