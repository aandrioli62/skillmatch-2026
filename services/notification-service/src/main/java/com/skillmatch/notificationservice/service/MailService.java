package com.skillmatch.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.from-display-name:SkillMatch}")
    private String fromDisplayName;

    // The SMTP *login* (spring.mail.username) authenticates with the relay, but is not
    // necessarily a deliverable address — the actual sender header needs the relay's
    // verified "from" address, configured separately.
    @Value("${notification.mail.from-address:}")
    private String fromAddress;

    /**
     * Sends a plain-text transactional email. Failures are logged and never thrown —
     * a broken mail relay must never stop a notification from being persisted.
     */
    public void send(String toAddress, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromDisplayName, fromAddress));
            message.setTo(toAddress);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent: to={}, subject=\"{}\"", toAddress, subject);
        } catch (Exception ex) {
            log.error("Failed to send email: to={}, subject=\"{}\"", toAddress, subject, ex);
        }
    }
}
