package com.skillmatch.notificationservice.service;

import com.skillmatch.notificationservice.client.UserServiceClient;
import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.event.IncomingEvent;
import com.skillmatch.notificationservice.exception.NotificationNotFoundException;
import com.skillmatch.notificationservice.mapper.NotificationMapper;
import com.skillmatch.notificationservice.model.Notification;
import com.skillmatch.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    // Event types important enough that the recipient also gets a real email, on top
    // of the in-app notification every event type already gets. Chosen deliberately —
    // not every event, to avoid spamming inboxes (and burning the SMTP provider's free-tier quota).
    private static final Set<String> EMAIL_EVENT_TYPES = Set.of(
            "user.validated", "candidature.accepted", "payment.completed");

    private static final Map<String, String> EMAIL_SUBJECTS = Map.of(
            "user.validated", "Il tuo profilo e' stato validato",
            "candidature.accepted", "Aggiornamento sulla tua candidatura",
            "payment.completed", "Pagamento elaborato");

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserServiceClient userServiceClient;
    private final MailService mailService;

    @Override
    public void processEvent(IncomingEvent event) {
        Map<String, Object> data = event.getData();
        if (data == null) {
            log.warn("Event eventType={} has no data payload, skipping", event.getEventType());
            return;
        }

        for (Recipient recipient : resolveRecipients(event.getEventType(), data)) {
            Notification notification = new Notification();
            notification.setEventType(event.getEventType());
            notification.setRecipientId(recipient.id());
            notification.setMessage(recipient.message());
            notification.setData(data);
            notification.setCreatedAt(Instant.now());
            notificationRepository.save(notification);

            log.info("Notification sent: recipientId={}, eventType={}, message=\"{}\"",
                    recipient.id(), event.getEventType(), recipient.message());

            if (recipient.id() != null && EMAIL_EVENT_TYPES.contains(event.getEventType())) {
                sendEmailSafely(event.getEventType(), recipient);
            }
        }
    }

    private void sendEmailSafely(String eventType, Recipient recipient) {
        try {
            String email = userServiceClient.getUserEmail(recipient.id());
            mailService.send(email, EMAIL_SUBJECTS.getOrDefault(eventType, "Aggiornamento da SkillMatch"), recipient.message());
        } catch (Exception ex) {
            log.error("Could not send email for eventType={}, recipientId={}: {}",
                    eventType, recipient.id(), ex.getMessage());
        }
    }

    @Override
    public List<NotificationResponse> listMine(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse markAsRead(String notificationId, UUID recipientId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!recipientId.equals(notification.getRecipientId())) {
            throw new AccessDeniedException(
                    "Notification with id=" + notificationId + " does not belong to the caller");
        }

        notification.setRead(true);
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    // =========================================================================
    // Event -> recipients/message mapping
    // =========================================================================

    private List<Recipient> resolveRecipients(String eventType, Map<String, Object> data) {
        return switch (eventType) {
            case "user.registered" -> List.of(new Recipient(
                    uuid(data, "userId"),
                    "Registrazione completata. Benvenuto su SkillMatch!"));

            case "user.validated" -> List.of(new Recipient(
                    uuid(data, "userId"),
                    "Il tuo profilo professionale e' stato validato da un amministratore."));

            case "project.published" -> List.of(new Recipient(
                    uuid(data, "companyId"),
                    "Il tuo progetto \"" + data.get("title") + "\" e' stato pubblicato ed e' visibile ai professionisti."));

            case "candidature.submitted" -> List.of(new Recipient(
                    uuid(data, "companyId"),
                    "Hai ricevuto una nuova candidatura per il progetto \"" + data.get("projectTitle") + "\"."));

            case "candidature.accepted" -> List.of(
                    new Recipient(uuid(data, "professionalId"), "La tua candidatura e' stata accettata!"),
                    new Recipient(uuid(data, "companyId"), "Hai selezionato un candidato per il tuo progetto."));

            case "project.completed" -> List.of(
                    new Recipient(uuid(data, "companyId"), "Il tuo progetto e' stato segnato come completato."),
                    new Recipient(uuid(data, "professionalId"), "Il progetto a cui hai lavorato e' stato completato."));

            case "payment.completed" -> List.of(
                    new Recipient(uuid(data, "companyId"), "Pagamento elaborato e fattura generata."),
                    new Recipient(uuid(data, "professionalId"), "Hai ricevuto un pagamento per il progetto completato."));

            case "feedback.aggregated" -> List.of(new Recipient(
                    uuid(data, "professionalId"),
                    "La tua reputazione e' stata aggiornata: media " + data.get("avgRating")
                            + " su " + data.get("totalReviews") + " recensioni."));

            default -> {
                log.info("No notification template for eventType={}, storing a generic entry", eventType);
                yield List.of(new Recipient(null, "Evento ricevuto: " + eventType));
            }
        };
    }

    private UUID uuid(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value == null ? null : UUID.fromString(value.toString());
    }

    private record Recipient(UUID id, String message) {
    }
}
