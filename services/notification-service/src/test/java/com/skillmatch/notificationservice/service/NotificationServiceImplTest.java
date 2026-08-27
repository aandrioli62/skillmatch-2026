package com.skillmatch.notificationservice.service;

import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.event.IncomingEvent;
import com.skillmatch.notificationservice.mapper.NotificationMapper;
import com.skillmatch.notificationservice.model.Notification;
import com.skillmatch.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl — Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private IncomingEvent eventOf(String eventType, Map<String, Object> data) {
        IncomingEvent event = new IncomingEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setData(data);
        return event;
    }

    @Nested
    @DisplayName("processEvent()")
    class ProcessEvent {

        @Test
        @DisplayName("user.registered: saves one notification for the registered user")
        void userRegistered_singleRecipient() {
            UUID userId = UUID.randomUUID();
            notificationService.processEvent(eventOf("user.registered", Map.of("userId", userId.toString())));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getRecipientId()).isEqualTo(userId);
            assertThat(captor.getValue().getEventType()).isEqualTo("user.registered");
            assertThat(captor.getValue().getMessage()).containsIgnoringCase("benvenuto");
        }

        @Test
        @DisplayName("candidature.accepted: saves one notification for the professional and one for the company")
        void candidatureAccepted_twoRecipients() {
            UUID professionalId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            notificationService.processEvent(eventOf("candidature.accepted", Map.of(
                    "professionalId", professionalId.toString(),
                    "companyId", companyId.toString())));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository, times(2)).save(captor.capture());
            List<UUID> recipientIds = captor.getAllValues().stream().map(Notification::getRecipientId).toList();
            assertThat(recipientIds).containsExactlyInAnyOrder(professionalId, companyId);
        }

        @Test
        @DisplayName("payment.completed: saves one notification for the company and one for the professional")
        void paymentCompleted_twoRecipients() {
            UUID companyId = UUID.randomUUID();
            UUID professionalId = UUID.randomUUID();
            notificationService.processEvent(eventOf("payment.completed", Map.of(
                    "companyId", companyId.toString(),
                    "professionalId", professionalId.toString())));

            verify(notificationRepository, times(2)).save(any(Notification.class));
        }

        @Test
        @DisplayName("feedback.aggregated: message includes the average rating and review count")
        void feedbackAggregated_includesStats() {
            UUID professionalId = UUID.randomUUID();
            notificationService.processEvent(eventOf("feedback.aggregated", Map.of(
                    "professionalId", professionalId.toString(),
                    "avgRating", "4.67",
                    "totalReviews", 3)));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getMessage()).contains("4.67").contains("3");
        }

        @Test
        @DisplayName("unknown event type: still saves a generic, unaddressed notification")
        void unknownEventType_savesGenericNotification() {
            notificationService.processEvent(eventOf("some.future.event", Map.of("foo", "bar")));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getRecipientId()).isNull();
            assertThat(captor.getValue().getMessage()).contains("some.future.event");
        }

        @Test
        @DisplayName("null data payload: skips without saving anything")
        void nullData_skipped() {
            notificationService.processEvent(eventOf("user.registered", null));

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listMine()")
    class ListMine {

        @Test
        @DisplayName("returns mapped notifications for the recipient")
        void listMine_returnsMapped() {
            UUID recipientId = UUID.randomUUID();
            Notification notification = new Notification();
            when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId))
                    .thenReturn(List.of(notification));
            when(notificationMapper.toResponse(notification)).thenReturn(new NotificationResponse());

            List<NotificationResponse> result = notificationService.listMine(recipientId);

            assertThat(result).hasSize(1);
        }
    }
}
