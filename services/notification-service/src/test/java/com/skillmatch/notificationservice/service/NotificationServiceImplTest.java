package com.skillmatch.notificationservice.service;

import com.skillmatch.notificationservice.client.UserServiceClient;
import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.event.IncomingEvent;
import com.skillmatch.notificationservice.exception.NotificationNotFoundException;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private MailService mailService;

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
        @DisplayName("candidature.submitted: saves one notification for the company, naming the project")
        void candidatureSubmitted_singleRecipient() {
            UUID companyId = UUID.randomUUID();
            notificationService.processEvent(eventOf("candidature.submitted", Map.of(
                    "companyId", companyId.toString(),
                    "projectTitle", "Consulenza UI/UX")));

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getRecipientId()).isEqualTo(companyId);
            assertThat(captor.getValue().getMessage()).contains("Consulenza UI/UX");
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
    @DisplayName("processEvent() — real email dispatch")
    class ProcessEventEmail {

        @Test
        @DisplayName("user.validated: also sends a real email to the professional")
        void userValidated_sendsEmail() {
            UUID userId = UUID.randomUUID();
            when(userServiceClient.getUserEmail(userId)).thenReturn("prof@example.com");

            notificationService.processEvent(eventOf("user.validated", Map.of("userId", userId.toString())));

            verify(mailService).send(eq("prof@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("candidature.accepted: sends an email to both the professional and the company")
        void candidatureAccepted_sendsEmailToBoth() {
            UUID professionalId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            when(userServiceClient.getUserEmail(professionalId)).thenReturn("prof@example.com");
            when(userServiceClient.getUserEmail(companyId)).thenReturn("company@example.com");

            notificationService.processEvent(eventOf("candidature.accepted", Map.of(
                    "professionalId", professionalId.toString(),
                    "companyId", companyId.toString())));

            verify(mailService).send(eq("prof@example.com"), anyString(), anyString());
            verify(mailService).send(eq("company@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("payment.completed: sends an email to both parties")
        void paymentCompleted_sendsEmailToBoth() {
            UUID companyId = UUID.randomUUID();
            UUID professionalId = UUID.randomUUID();
            when(userServiceClient.getUserEmail(companyId)).thenReturn("company@example.com");
            when(userServiceClient.getUserEmail(professionalId)).thenReturn("prof@example.com");

            notificationService.processEvent(eventOf("payment.completed", Map.of(
                    "companyId", companyId.toString(),
                    "professionalId", professionalId.toString())));

            verify(mailService, times(2)).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("candidature.submitted: in-app only, no email sent")
        void candidatureSubmitted_noEmail() {
            UUID companyId = UUID.randomUUID();
            notificationService.processEvent(eventOf("candidature.submitted", Map.of(
                    "companyId", companyId.toString(),
                    "projectTitle", "Consulenza UI/UX")));

            verify(mailService, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("email lookup failure: notification is still saved, exception does not propagate")
        void emailLookupFails_doesNotBreakNotification() {
            UUID userId = UUID.randomUUID();
            when(userServiceClient.getUserEmail(userId)).thenThrow(new RuntimeException("User Service unavailable"));

            notificationService.processEvent(eventOf("user.validated", Map.of("userId", userId.toString())));

            verify(notificationRepository).save(any(Notification.class));
            verify(mailService, never()).send(anyString(), anyString(), anyString());
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

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("owned notification: marks it read and returns the mapped response")
        void markAsRead_owned_marksRead() {
            UUID recipientId = UUID.randomUUID();
            Notification notification = new Notification();
            notification.setRecipientId(recipientId);
            when(notificationRepository.findById("n1")).thenReturn(Optional.of(notification));
            when(notificationRepository.save(notification)).thenReturn(notification);
            when(notificationMapper.toResponse(notification)).thenReturn(new NotificationResponse());

            notificationService.markAsRead("n1", recipientId);

            assertThat(notification.isRead()).isTrue();
            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("unknown id: throws NotificationNotFoundException")
        void markAsRead_notFound_throws() {
            when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead("missing", UUID.randomUUID()))
                    .isInstanceOf(NotificationNotFoundException.class);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("notification belongs to another recipient: throws AccessDeniedException")
        void markAsRead_notOwner_throws() {
            Notification notification = new Notification();
            notification.setRecipientId(UUID.randomUUID());
            when(notificationRepository.findById("n1")).thenReturn(Optional.of(notification));

            assertThatThrownBy(() -> notificationService.markAsRead("n1", UUID.randomUUID()))
                    .isInstanceOf(AccessDeniedException.class);

            verify(notificationRepository, never()).save(any());
        }
    }
}
