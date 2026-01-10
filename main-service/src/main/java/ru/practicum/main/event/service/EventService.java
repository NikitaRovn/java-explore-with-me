package ru.practicum.main.event.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.main.event.dto.PublicEventSearchRequest;
import ru.practicum.main.event.enums.EventState;
import ru.practicum.main.event.model.UpdateEventAdminRequest;
import ru.practicum.main.event.model.UpdateEventUserRequest;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.request.model.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.model.EventRequestStatusUpdateResult;
import ru.practicum.main.request.dto.ParticipationRequestDto;

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

    List<EventShortDto> getPublicEvents(PublicEventSearchRequest request, HttpServletRequest servletRequest);

    EventFullDto getPublicEvent(long eventId, HttpServletRequest request);
}