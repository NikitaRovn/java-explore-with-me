package ru.practicum.main.request;

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