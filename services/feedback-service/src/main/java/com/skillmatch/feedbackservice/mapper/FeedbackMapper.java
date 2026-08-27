package com.skillmatch.feedbackservice.mapper;

import com.skillmatch.feedbackservice.dto.response.FeedbackResponse;
import com.skillmatch.feedbackservice.model.Feedback;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    FeedbackResponse toResponse(Feedback feedback);
}
