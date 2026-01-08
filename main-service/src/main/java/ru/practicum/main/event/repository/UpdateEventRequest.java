package ru.practicum.main.event.repository;

import ru.practicum.main.event.model.Location;

import java.time.LocalDateTime;

public interface UpdateEventRequest {
    String getAnnotation();

    Long getCategory();

    String getDescription();

    LocalDateTime getEventDate();

    Location getLocation();

    Boolean getPaid();

    Integer getParticipantLimit();

    Boolean getRequestModeration();

    String getTitle();
}