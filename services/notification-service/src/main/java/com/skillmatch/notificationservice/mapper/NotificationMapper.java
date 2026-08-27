package com.skillmatch.notificationservice.mapper;

import com.skillmatch.notificationservice.dto.response.NotificationResponse;
import com.skillmatch.notificationservice.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
