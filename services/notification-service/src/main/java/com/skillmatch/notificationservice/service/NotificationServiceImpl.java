package com.skillmatch.notificationservice.service;

import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.event.IncomingEvent;
import com.skillmatch.notificationservice.mapper.NotificationMapper;
import com.skillmatch.notificationservice.model.Notification;
import com.skillmatch.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

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

            // Mock the email channel (Fase 5): "sending" a notification just means logging it.
            log.info("Notification sent: recipientId={}, eventType={}, message=\"{}\"",
                    recipient.id(), event.getEventType(), recipient.message());
        }
    }

    @Override
    public List<NotificationResponse> listMine(UUID recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
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
