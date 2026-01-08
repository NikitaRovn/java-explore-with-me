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
        return new ParticipationRequestDto(
                request.getCreated(),
                request.getEvent().getId(),
                request.getId(),
                request.getRequester().getId(),
                request.getStatus()
        );
    }
}