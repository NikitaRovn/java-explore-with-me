package ru.practicum.main.event.mapper;

import ru.practicum.main.category.mapper.CategoryMapper;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.enums.EventState;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.user.mapper.UserMapper;
import ru.practicum.main.user.model.User;

import java.time.LocalDateTime;

public final class EventMapper {
    private EventMapper() {
    }

    public static EventFullDto toFullDto(Event event) {
        return toFullDto(event, 0, 0);
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


    public static Event toEvent(NewEventDto dto, Category category, User user) {
        if (dto == null) {
            return null;
        }

        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .createdOn(LocalDateTime.now())
                .initiator(user)
                .location(dto.getLocation())
                .paid(Boolean.TRUE.equals(dto.getPaid()))
                .participantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration() == null || dto.getRequestModeration())
                .state(EventState.PENDING)
                .title(dto.getTitle())
                .build();
    }
}