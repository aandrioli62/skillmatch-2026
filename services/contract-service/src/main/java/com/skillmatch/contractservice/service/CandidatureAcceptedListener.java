package com.skillmatch.contractservice.service;

import com.skillmatch.contractservice.config.RabbitMQConfig;
import com.skillmatch.contractservice.event.CandidatureAcceptedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandidatureAcceptedListener {

    private final ContractService contractService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CANDIDATURE_ACCEPTED)
    public void onCandidatureAccepted(CandidatureAcceptedEvent event) {
        log.info("Received candidature.accepted event: candidatureId={}, projectId={}",
                event.getData().getCandidatureId(), event.getData().getProjectId());
        contractService.createFromCandidatureAccepted(event.getData());
    }
}
