package ru.practicum.main.request;

import java.util.List;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getUserRequests(long userId);

    ParticipationRequestDto addRequest(long userId, long eventId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);
}