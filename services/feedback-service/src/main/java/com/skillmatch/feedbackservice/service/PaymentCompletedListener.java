package com.skillmatch.feedbackservice.service;

import com.skillmatch.feedbackservice.config.RabbitMQConfig;
import com.skillmatch.feedbackservice.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedListener {

    private final FeedbackService feedbackService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_COMPLETED)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        PaymentCompletedEvent.Data data = event.getData();
        log.info("Received payment.completed event: projectId={}, companyId={}, professionalId={}",
                data.getProjectId(), data.getCompanyId(), data.getProfessionalId());
        feedbackService.enableFeedback(data.getProjectId(), data.getCompanyId(), data.getProfessionalId());
    }
}
