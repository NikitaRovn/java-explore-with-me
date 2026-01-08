package ru.practicum.main.event.mapper;

import ru.practicum.main.category.mapper.CategoryMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.user.mapper.UserMapper;

public final class EventMapper {
    private EventMapper() {
    }

    public static EventFullDto toFullDto(Event event, long confirmedRequests, long views) {
        if (event == null) {
            return null;
        }
        return new EventFullDto(
                event.getAnnotation(),
                CategoryMapper.toDto(event.getCategory()),
                confirmedRequests,
                event.getCreatedOn(),
                event.getDescription(),
                event.getEventDate(),
                event.getId(),
                UserMapper.toShortDto(event.getInitiator()),
                event.getLocation(),
                event.isPaid(),
                event.getParticipantLimit(),
                event.getPublishedOn(),
                event.isRequestModeration(),
                event.getState(),
                event.getTitle(),
                views
        );
    }

    public static EventShortDto toShortDto(Event event, long confirmedRequests, long views) {
        if (event == null) {
            return null;
        }
        return new EventShortDto(
                event.getAnnotation(),
                CategoryMapper.toDto(event.getCategory()),
                confirmedRequests,
                event.getEventDate(),
                event.getId(),
                UserMapper.toShortDto(event.getInitiator()),
                event.isPaid(),
                event.getTitle(),
                views
        );
    }
}