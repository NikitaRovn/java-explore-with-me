package ru.practicum.main.request.mapper;

import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.dto.ParticipationRequestDto;

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
}