package com.skillmatch.feedbackservice.service;

import com.skillmatch.feedbackservice.dto.response.FeedbackResponse;
import com.skillmatch.feedbackservice.event.FeedbackAggregatedEvent;
import com.skillmatch.feedbackservice.exception.FeedbackNotFoundException;
import com.skillmatch.feedbackservice.exception.InvalidFeedbackOperationException;
import com.skillmatch.feedbackservice.mapper.FeedbackMapper;
import com.skillmatch.feedbackservice.model.Feedback;
import com.skillmatch.feedbackservice.model.FeedbackEligibility;
import com.skillmatch.feedbackservice.repository.FeedbackEligibilityRepository;
import com.skillmatch.feedbackservice.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackServiceImpl — Unit Tests")
class FeedbackServiceImplTest {

    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackEligibilityRepository eligibilityRepository;
    @Mock
    private EventPublisherService eventPublisher;
    @Mock
    private FeedbackMapper feedbackMapper;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    private UUID projectId;
    private UUID companyId;
    private UUID professionalId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        professionalId = UUID.randomUUID();
    }

    private FeedbackEligibility eligibility() {
        FeedbackEligibility e = new FeedbackEligibility();
        e.setProjectId(projectId);
        e.setCompanyId(companyId);
        e.setProfessionalId(professionalId);
        return e;
    }

    // =========================================================================
    // enableFeedback
    // =========================================================================

    @Nested
    @DisplayName("enableFeedback()")
    class EnableFeedback {

        @Test
        @DisplayName("no existing eligibility: saves a new row")
        void enableFeedback_success() {
            when(eligibilityRepository.existsByProjectId(projectId)).thenReturn(false);

            feedbackService.enableFeedback(projectId, companyId, professionalId);

            ArgumentCaptor<FeedbackEligibility> captor = ArgumentCaptor.forClass(FeedbackEligibility.class);
            verify(eligibilityRepository).save(captor.capture());
            assertThat(captor.getValue().getProjectId()).isEqualTo(projectId);
            assertThat(captor.getValue().getCompanyId()).isEqualTo(companyId);
            assertThat(captor.getValue().getProfessionalId()).isEqualTo(professionalId);
        }

        @Test
        @DisplayName("eligibility already exists: skips without saving (idempotent)")
        void enableFeedback_duplicate_skipped() {
            when(eligibilityRepository.existsByProjectId(projectId)).thenReturn(true);

            feedbackService.enableFeedback(projectId, companyId, professionalId);

            verify(eligibilityRepository, never()).save(any());
        }
    }

    // =========================================================================
    // submitFeedback
    // =========================================================================

    @Nested
    @DisplayName("submitFeedback()")
    class SubmitFeedback {

        @Test
        @DisplayName("company reviews professional: saves feedback and publishes feedback.aggregated")
        void submitFeedback_companyReviewsProfessional_publishesAggregation() {
            when(eligibilityRepository.findByProjectId(projectId)).thenReturn(Optional.of(eligibility()));
            when(feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(projectId, companyId, professionalId))
                    .thenReturn(false);
            Feedback saved = new Feedback();
            saved.setId(UUID.randomUUID());
            saved.setRating(5);
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);
            when(feedbackMapper.toResponse(saved)).thenReturn(new FeedbackResponse());

            Feedback existing = new Feedback();
            existing.setRating(4);
            // the just-saved feedback (rating 5) is included alongside the pre-existing one (rating 4)
            when(feedbackRepository.findByRevieweeId(professionalId)).thenReturn(List.of(existing, saved));

            feedbackService.submitFeedback(companyId, projectId, 5, "Ottimo lavoro");

            ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
            verify(feedbackRepository).save(captor.capture());
            assertThat(captor.getValue().getReviewerId()).isEqualTo(companyId);
            assertThat(captor.getValue().getRevieweeId()).isEqualTo(professionalId);
            assertThat(captor.getValue().getRating()).isEqualTo(5);

            ArgumentCaptor<FeedbackAggregatedEvent> eventCaptor = ArgumentCaptor.forClass(FeedbackAggregatedEvent.class);
            verify(eventPublisher).publishFeedbackAggregated(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getData().getProfessionalId()).isEqualTo(professionalId);
            assertThat(eventCaptor.getValue().getData().getTotalReviews()).isEqualTo(2);
            assertThat(eventCaptor.getValue().getData().getAvgRating()).isEqualByComparingTo(new BigDecimal("4.50"));
        }

        @Test
        @DisplayName("professional reviews company: saves feedback but does not publish aggregation")
        void submitFeedback_professionalReviewsCompany_noAggregation() {
            when(eligibilityRepository.findByProjectId(projectId)).thenReturn(Optional.of(eligibility()));
            when(feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(projectId, professionalId, companyId))
                    .thenReturn(false);
            Feedback saved = new Feedback();
            saved.setId(UUID.randomUUID());
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);
            when(feedbackMapper.toResponse(saved)).thenReturn(new FeedbackResponse());

            feedbackService.submitFeedback(professionalId, projectId, 4, "Buona collaborazione");

            ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
            verify(feedbackRepository).save(captor.capture());
            assertThat(captor.getValue().getReviewerId()).isEqualTo(professionalId);
            assertThat(captor.getValue().getRevieweeId()).isEqualTo(companyId);

            verify(eventPublisher, never()).publishFeedbackAggregated(any());
            verify(feedbackRepository, never()).findByRevieweeId(any());
        }

        @Test
        @DisplayName("rounds average rating to 2 decimal places (HALF_UP)")
        void submitFeedback_roundsAverage() {
            when(eligibilityRepository.findByProjectId(projectId)).thenReturn(Optional.of(eligibility()));
            when(feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(projectId, companyId, professionalId))
                    .thenReturn(false);
            Feedback saved = new Feedback();
            saved.setId(UUID.randomUUID());
            saved.setRating(4);
            when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);
            when(feedbackMapper.toResponse(saved)).thenReturn(new FeedbackResponse());

            Feedback r1 = new Feedback();
            r1.setRating(5);
            Feedback r2 = new Feedback();
            r2.setRating(5);
            // (5 + 5 + 4) / 3 = 4.6666... -> rounds to 4.67
            when(feedbackRepository.findByRevieweeId(professionalId)).thenReturn(List.of(r1, r2, saved));

            feedbackService.submitFeedback(companyId, projectId, 4, null);

            ArgumentCaptor<FeedbackAggregatedEvent> eventCaptor = ArgumentCaptor.forClass(FeedbackAggregatedEvent.class);
            verify(eventPublisher).publishFeedbackAggregated(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getData().getAvgRating()).isEqualByComparingTo(new BigDecimal("4.67"));
            assertThat(eventCaptor.getValue().getData().getTotalReviews()).isEqualTo(3);
        }

        @Test
        @DisplayName("project not eligible: throws InvalidFeedbackOperationException")
        void submitFeedback_notEligible_throws() {
            when(eligibilityRepository.findByProjectId(projectId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.submitFeedback(companyId, projectId, 5, null))
                    .isInstanceOf(InvalidFeedbackOperationException.class)
                    .hasMessageContaining("not yet eligible");

            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("caller is not a party to the project: throws InvalidFeedbackOperationException")
        void submitFeedback_notAParty_throws() {
            when(eligibilityRepository.findByProjectId(projectId)).thenReturn(Optional.of(eligibility()));

            assertThatThrownBy(() -> feedbackService.submitFeedback(UUID.randomUUID(), projectId, 5, null))
                    .isInstanceOf(InvalidFeedbackOperationException.class)
                    .hasMessageContaining("not a party");

            verify(feedbackRepository, never()).save(any());
        }

        @Test
        @DisplayName("already reviewed: throws InvalidFeedbackOperationException")
        void submitFeedback_alreadyReviewed_throws() {
            when(eligibilityRepository.findByProjectId(projectId)).thenReturn(Optional.of(eligibility()));
            when(feedbackRepository.existsByProjectIdAndReviewerIdAndRevieweeId(projectId, companyId, professionalId))
                    .thenReturn(true);

            assertThatThrownBy(() -> feedbackService.submitFeedback(companyId, projectId, 5, null))
                    .isInstanceOf(InvalidFeedbackOperationException.class)
                    .hasMessageContaining("already reviewed");

            verify(feedbackRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Retrieval
    // =========================================================================

    @Nested
    @DisplayName("getFeedback() / listing")
    class Retrieval {

        @Test
        @DisplayName("unknown feedback: throws FeedbackNotFoundException")
        void getFeedback_notFound() {
            UUID feedbackId = UUID.randomUUID();
            when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> feedbackService.getFeedback(feedbackId))
                    .isInstanceOf(FeedbackNotFoundException.class);
        }

        @Test
        @DisplayName("listByProject: returns mapped list")
        void listByProject_returnsMapped() {
            Feedback feedback = new Feedback();
            when(feedbackRepository.findByProjectId(projectId)).thenReturn(List.of(feedback));
            when(feedbackMapper.toResponse(feedback)).thenReturn(new FeedbackResponse());

            List<FeedbackResponse> result = feedbackService.listByProject(projectId);

            assertThat(result).hasSize(1);
        }
    }
}
