package com.skillmatch.userservice.service;

import com.skillmatch.userservice.event.FeedbackAggregatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedbackEventListenerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private FeedbackEventListener feedbackEventListener;

    @Test
    void handleFeedbackAggregated_delegatesToUpdateReputation() {
        UUID professionalId = UUID.randomUUID();

        FeedbackAggregatedEvent.Data data = new FeedbackAggregatedEvent.Data();
        data.setProfessionalId(professionalId);
        data.setAvgRating(new BigDecimal("4.20"));
        data.setTotalReviews(6);

        FeedbackAggregatedEvent event = new FeedbackAggregatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("feedback.aggregated");
        event.setSource("feedback-service");
        event.setData(data);

        feedbackEventListener.handleFeedbackAggregated(event);

        verify(userService).updateReputation(professionalId, new BigDecimal("4.20"), 6);
    }
}
