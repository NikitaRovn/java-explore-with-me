package ru.practicum.main.request.mapper;

import ru.practicum.main.event.model.Event;
import ru.practicum.main.request.enums.RequestStatus;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.user.model.User;

import java.time.LocalDateTime;

public final class ParticipationRequestMapper {
    private ParticipationRequestMapper() {
    }

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        return ParticipationRequestDto.builder()
                .created(request.getCreated())
                .event(request.getEvent() != null ? request.getEvent().getId() : null)
                .id(request.getId())
                .requester(request.getRequester() != null ? request.getRequester().getId() : null)
                .status(request.getStatus())
                .build();
    }

    public static ParticipationRequest toEntity(Event event, User requester, RequestStatus status, LocalDateTime created) {
        ParticipationRequest request = new ParticipationRequest();
        request.setEvent(event);
        request.setRequester(requester);
        request.setStatus(status);
        request.setCreated(created);
        return request;
    }
}