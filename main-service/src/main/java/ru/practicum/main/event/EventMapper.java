package ru.practicum.main.event;

import ru.practicum.main.category.CategoryMapper;
import ru.practicum.main.user.UserMapper;

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