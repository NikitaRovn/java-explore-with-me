package ru.practicum.main.event.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.PublicEventSearchRequest;
import ru.practicum.main.event.enums.EventAdminStateAction;
import ru.practicum.main.event.enums.EventSort;
import ru.practicum.main.event.enums.EventState;
import ru.practicum.main.event.enums.EventUserStateAction;
import ru.practicum.main.event.model.UpdateEventAdminRequest;
import ru.practicum.main.event.repository.UpdateEventRequest;
import ru.practicum.main.event.model.UpdateEventUserRequest;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.ForbiddenException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.model.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.model.EventRequestStatusUpdateResult;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.mapper.ParticipationRequestMapper;
import ru.practicum.main.request.repository.ParticipationRequestRepository;
import ru.practicum.main.request.enums.RequestCount;
import ru.practicum.main.request.enums.RequestStatus;
import ru.practicum.main.request.enums.RequestUpdateStatus;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.stats.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.main.utility.Constant.CATEGORY_NOT_FOUND;
import static ru.practicum.main.utility.Constant.DEFAULT_START;
import static ru.practicum.main.utility.Constant.EVENT_NOT_FOUND;
import static ru.practicum.main.utility.Constant.FORMATTER;
import static ru.practicum.main.utility.Constant.NOT_INITIATOR;
import static ru.practicum.main.utility.Constant.USER_NOT_FOUND;

@Service
@Transactional
public class EventServiceImpl implements EventService {
    private static final String EVENT_DATE = "eventDate";

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    public EventServiceImpl(EventRepository eventRepository,
                            UserRepository userRepository,
                            CategoryRepository categoryRepository,
                            ParticipationRequestRepository requestRepository,
                            StatsClient statsClient) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.requestRepository = requestRepository;
        this.statsClient = statsClient;
    }

    @Override
    public EventFullDto addEvent(long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format(USER_NOT_FOUND, userId)));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND, dto.getCategory())));
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть минимум через 2 часа.");
        }
        Event event = EventMapper.toEvent(dto, category, user);
        Event saved = eventRepository.save(event);
        return EventMapper.toFullDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        ensureUserExists(userId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        return EventMapper.toShortDtos(events, getConfirmedRequests(events), getViews(events));
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(NOT_INITIATOR);
        }
        return EventMapper.toFullDto(event, getConfirmedRequests(List.of(event)), getViews(List.of(event)));
    }

    @Override
    public EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(NOT_INITIATOR);
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Опубликованное событие нельзя изменить.");
        }
        applyUserUpdate(event, request);
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть минимум через 2 часа.");
        }
        Event saved = eventRepository.save(event);
        return EventMapper.toFullDto(saved, getConfirmedRequests(List.of(saved)), getViews(List.of(saved)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(NOT_INITIATOR);
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(long userId, long eventId,
                                                               EventRequestStatusUpdateRequest request) {
        Event event = getEventWithInitiatorCheck(userId, eventId);
        List<Long> requestIds = request.getRequestIds();
        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);
        validatePendingRequests(eventId, requests);
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        RequestUpdateOutcome outcome = processStatusUpdate(event, requests, request.getStatus(), confirmedCount);
        rejectRemainingPendingIfLimitReached(event, requestIds, outcome);
        requestRepository.saveAll(outcome.getToSave());
        return new EventRequestStatusUpdateResult(outcome.getConfirmed(), outcome.getRejected());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             String rangeStart, String rangeEnd, int from, int size) {
        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Диапазон дат указан неверно.");
        }
        Specification<Event> specification = buildAdminSpecification(users, states, categories, start, end);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();
        return EventMapper.toFullDtos(events, getConfirmedRequests(events), getViews(events));
    }

    @Override
    public EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        applyAdminUpdate(event, request);
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Дата события должна быть минимум через час.");
        }
        Event saved = eventRepository.save(event);
        return EventMapper.toFullDto(saved, getConfirmedRequests(List.of(saved)), getViews(List.of(saved)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(PublicEventSearchRequest request,
                                               HttpServletRequest servletRequest) {
        LocalDateTime start = parseDate(request.getRangeStart());
        LocalDateTime end = parseDate(request.getRangeEnd());
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Диапазон дат указан неверно.");
        }
        if (start == null && end == null) {
            start = LocalDateTime.now();
        }
        Specification<Event> specification = buildPublicSpecification(request.getText(), request.getCategories(),
                request.getPaid(), start, end);

        List<Event> events;
        if (request.getSort() == EventSort.VIEWS) {
            events = eventRepository.findAll(specification);
        } else {
            Pageable pageable = PageRequest.of(request.getFrom() / request.getSize(), request.getSize(),
                    Sort.by(EVENT_DATE));
            events = eventRepository.findAll(specification, pageable).getContent();
        }

        if (Boolean.TRUE.equals(request.getOnlyAvailable())) {
            Map<Long, Long> confirmedMap = getConfirmedRequests(events);
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0
                            || confirmedMap.getOrDefault(event.getId(), 0L) < event.getParticipantLimit())
                    .toList();
        }

        List<EventShortDto> result = EventMapper.toShortDtos(events, getConfirmedRequests(events), getViews(events));
        if (Boolean.TRUE.equals(request.getOnlyAvailable())) {
            result = result.stream()
                    .sorted(Comparator.comparingLong(EventShortDto::getViews).reversed())
                    .toList();
            int startIndex = Math.min(request.getFrom(), result.size());
            int endIndex = Math.min(request.getFrom() + request.getSize(), result.size());
            result = result.subList(startIndex, endIndex);
        }
        statsClient.addHit(servletRequest);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        }
        statsClient.addHit(request);
        return EventMapper.toFullDto(event, getConfirmedRequests(List.of(event)), getViews(List.of(event)));
    }

    private void applyUserUpdate(Event event, UpdateEventUserRequest request) {

        applyCommonUpdate(event, request);
        applyUserStateAction(event, request);
    }

    private void applyAdminUpdate(Event event, UpdateEventAdminRequest request) {
        applyCommonUpdate(event, request);
        applyAdminStateAction(event, request);
    }

    private void applyCommonUpdate(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND, request.getCategory())));
            event.setCategory(category);
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    private void applyUserStateAction(Event event, UpdateEventUserRequest request) {
        if (request.getStateAction() == EventUserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        }
        if (request.getStateAction() == EventUserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
    }

    private void applyAdminStateAction(Event event, UpdateEventAdminRequest request) {
        if (request.getStateAction() == EventAdminStateAction.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Событие не в статусе ожидания публикации.");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        }
        if (request.getStateAction() == EventAdminStateAction.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Опубликованное событие нельзя отклонить.");
            }
            event.setState(EventState.CANCELED);
        }
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        List<RequestCount> counts = requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);
        Map<Long, Long> result = new HashMap<>();
        for (RequestCount count : counts) {
            result.put(count.getEventId(), count.getTotal());
        }
        return result;
    }

    private Map<Long, Long> getViews(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        String end = LocalDateTime.now().format(FORMATTER);
        Map<Long, Long> result = new HashMap<>();
        for (Event event : events) {
            String start = event.getPublishedOn() == null
                    ? DEFAULT_START
                    : event.getPublishedOn().format(FORMATTER);
            String uri = "/events/" + event.getId();
            List<ViewStatsDto> stats = statsClient.getStats(start, end, List.of(uri), true);
            long hits = stats.stream()
                    .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits))
                    .getOrDefault(uri, 0L);
            result.put(event.getId(), hits);
        }
        return result;
    }

    private Specification<Event> buildAdminSpecification(List<Long> users, List<EventState> states,
                                                         List<Long> categories, LocalDateTime start,
                                                         LocalDateTime end) {
        Specification<Event> specification = Specification.where(null);
        if (users != null && !users.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("initiator").get("id").in(users));
        }
        if (states != null && !states.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("state").in(states));
        }
        if (categories != null && !categories.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }
        if (start != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(EVENT_DATE), start));
        }
        if (end != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(EVENT_DATE), end));
        }
        return specification;
    }

    private Specification<Event> buildPublicSpecification(String text, List<Long> categories, Boolean paid,
                                                          LocalDateTime start, LocalDateTime end) {
        Specification<Event> specification = Specification.where((root, query, cb) -> cb.equal(root.get("state"),
                EventState.PUBLISHED));
        if (text != null && !text.isBlank()) {
            String like = "%" + text.toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("annotation")), like),
                    cb.like(cb.lower(root.get("description")), like)));
        }
        if (categories != null && !categories.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("category").get("id").in(categories));
        }
        if (paid != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("paid"), paid));
        }
        if (start != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(EVENT_DATE), start));
        }
        if (end != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get(EVENT_DATE), end));
        }
        return specification;
    }

    private LocalDateTime parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(date, FORMATTER);
        } catch (Exception exception) {
            throw new BadRequestException("Неверный формат даты.");
        }
    }

    private void ensureUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден.");
        }
    }


    private Event getEventWithInitiatorCheck(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenException(NOT_INITIATOR);
        }
        return event;
    }

    private void validatePendingRequests(long eventId, List<ParticipationRequest> requests) {
        for (ParticipationRequest participationRequest : requests) {
            if (!participationRequest.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка не относится к событию.");
            }
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у ожидающих заявок.");
            }
        }
    }

    private RequestUpdateOutcome processStatusUpdate(Event event, List<ParticipationRequest> requests,
                                                     RequestUpdateStatus updateStatus, long confirmedCount) {
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        List<ParticipationRequest> toSave = new ArrayList<>(requests);
        long currentConfirmed = confirmedCount;

        for (ParticipationRequest participationRequest : requests) {
            if (updateStatus == RequestUpdateStatus.CONFIRMED) {
                if (isLimitReached(event, currentConfirmed)) {
                    participationRequest.setStatus(RequestStatus.REJECTED);
                    rejected.add(ParticipationRequestMapper.toDto(participationRequest));
                } else {
                    participationRequest.setStatus(RequestStatus.CONFIRMED);
                    currentConfirmed++;
                    confirmed.add(ParticipationRequestMapper.toDto(participationRequest));
                }
            } else {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejected.add(ParticipationRequestMapper.toDto(participationRequest));
            }
        }

        return new RequestUpdateOutcome(toSave, confirmed, rejected, currentConfirmed);
    }

    private void rejectRemainingPendingIfLimitReached(Event event, List<Long> requestIds,
                                                      RequestUpdateOutcome outcome) {
        if (!isLimitReached(event, outcome.getConfirmedCount())) {
            return;
        }
        List<ParticipationRequest> pending = requestRepository.findByEventIdAndStatus(event.getId(),
                        RequestStatus.PENDING)
                .stream()
                .filter(pendingRequest -> !requestIds.contains(pendingRequest.getId()))
                .toList();
        for (ParticipationRequest participationRequest : pending) {
            participationRequest.setStatus(RequestStatus.REJECTED);
            outcome.getRejected().add(ParticipationRequestMapper.toDto(participationRequest));
        }
        outcome.getToSave().addAll(pending);
    }

    private boolean isLimitReached(Event event, long confirmedCount) {
        return event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit();
    }

    @Getter
    private static class RequestUpdateOutcome {
        private final List<ParticipationRequest> toSave;
        private final List<ParticipationRequestDto> confirmed;
        private final List<ParticipationRequestDto> rejected;
        private final long confirmedCount;

        private RequestUpdateOutcome(List<ParticipationRequest> toSave,
                                     List<ParticipationRequestDto> confirmed,
                                     List<ParticipationRequestDto> rejected,
                                     long confirmedCount) {
            this.toSave = toSave;
            this.confirmed = confirmed;
            this.rejected = rejected;
            this.confirmedCount = confirmedCount;
        }

    }
}