package ru.practicum.main.event;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.main.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.EventRequestStatusUpdateResult;
import ru.practicum.main.request.ParticipationRequestDto;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(long userId, NewEventDto dto);

    List<EventShortDto> getUserEvents(long userId, int from, int size);

    EventFullDto getUserEvent(long userId, long eventId);

    EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest request);

    List<ParticipationRequestDto> getEventParticipants(long userId, long eventId);

    EventRequestStatusUpdateResult updateRequestsStatus(long userId, long eventId,
                                                        EventRequestStatusUpdateRequest request);

    List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                      String rangeStart, String rangeEnd, int from, int size);

    EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                        String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                        EventSort sort, int from, int size, HttpServletRequest servletRequest);

    EventFullDto getPublicEvent(long eventId, HttpServletRequest request);
}