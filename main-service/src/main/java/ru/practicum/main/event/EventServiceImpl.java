package ru.practicum.main.event;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.Category;
import ru.practicum.main.category.CategoryRepository;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.EventRequestStatusUpdateResult;
import ru.practicum.main.request.ParticipationRequest;
import ru.practicum.main.request.ParticipationRequestDto;
import ru.practicum.main.request.ParticipationRequestMapper;
import ru.practicum.main.request.ParticipationRequestRepository;
import ru.practicum.main.request.RequestCount;
import ru.practicum.main.request.RequestStatus;
import ru.practicum.main.request.RequestUpdateStatus;
import ru.practicum.main.user.User;
import ru.practicum.main.user.UserRepository;
import ru.practicum.stats.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_START = "1970-01-01 00:00:00";
    private static final String CATEGORY_NOT_FOUND = "Категория с id=%d не найдена.";
    private static final String COMPILATION_NOT_FOUND = "Подборка с id=%d не найдена.";
    private static final String EVENT_NOT_FOUND = "Событие с id=%d не найдено.";
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
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден."));
        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND, dto.getCategory())));
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть минимум через 2 часа.");
        }
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(category);
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiator(user);
        event.setLocation(dto.getLocation());
        event.setPaid(Boolean.TRUE.equals(dto.getPaid()));
        event.setParticipantLimit(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit());
        event.setRequestModeration(dto.getRequestModeration() == null || dto.getRequestModeration());
        event.setState(EventState.PENDING);
        event.setTitle(dto.getTitle());
        Event saved = eventRepository.save(event);
        return EventMapper.toFullDto(saved, 0, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        ensureUserExists(userId);
        ensurePagination(from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        return toShortDtos(events);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        }
        return toFullDto(event);
    }

    @Override
    public EventFullDto updateUserEvent(long userId, long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Опубликованное событие нельзя изменить.");
        }
        applyUserUpdate(event, request);
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть минимум через 2 часа.");
        }
        return toFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventParticipants(long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(long userId, long eventId,
                                                               EventRequestStatusUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest participationRequest : requests) {
            if (!participationRequest.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Заявка не относится к событию.");
            }
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у ожидающих заявок.");
            }
            if (request.getStatus() == RequestUpdateStatus.CONFIRMED) {
                if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                    throw new ConflictException("Достигнут лимит участников.");
                }
                participationRequest.setStatus(RequestStatus.CONFIRMED);
                confirmedCount++;
                confirmed.add(ParticipationRequestMapper.toDto(participationRequest));
            } else {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejected.add(ParticipationRequestMapper.toDto(participationRequest));
            }
        }

        List<ParticipationRequest> toSave = new ArrayList<>(requests);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            List<ParticipationRequest> pending = requestRepository.findByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest participationRequest : pending) {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejected.add(ParticipationRequestMapper.toDto(participationRequest));
            }
            toSave.addAll(pending);
        }

        requestRepository.saveAll(toSave);
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             String rangeStart, String rangeEnd, int from, int size) {
        ensurePagination(from, size);
        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Диапазон дат указан неверно.");
        }
        Specification<Event> specification = buildAdminSpecification(users, states, categories, start, end);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();
        return toFullDtos(events);
    }

    @Override
    public EventFullDto updateAdminEvent(long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        applyAdminUpdate(event, request);
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new BadRequestException("Дата события должна быть минимум через час.");
        }
        return toFullDto(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                               EventSort sort, int from, int size, HttpServletRequest servletRequest) {
        ensurePagination(from, size);
        LocalDateTime start = parseDate(rangeStart);
        LocalDateTime end = parseDate(rangeEnd);
        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("Диапазон дат указан неверно.");
        }
        if (start == null && end == null) {
            start = LocalDateTime.now();
        }
        Specification<Event> specification = buildPublicSpecification(text, categories, paid, start, end);

        List<Event> events;
        if (sort == EventSort.VIEWS) {
            events = eventRepository.findAll(specification);
        } else {
            Pageable pageable = PageRequest.of(from / size, size, Sort.by(EVENT_DATE));
            events = eventRepository.findAll(specification, pageable).getContent();
        }

        if (Boolean.TRUE.equals(onlyAvailable)) {
            Map<Long, Long> confirmedMap = getConfirmedRequests(events);
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0
                            || confirmedMap.getOrDefault(event.getId(), 0L) < event.getParticipantLimit())
                    .toList();
        }

        List<EventShortDto> result = toShortDtos(events);
        if (sort == EventSort.VIEWS) {
            result = result.stream()
                    .sorted(Comparator.comparingLong(EventShortDto::getViews).reversed())
                    .toList();
            int startIndex = Math.min(from, result.size());
            int endIndex = Math.min(from + size, result.size());
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
        return toFullDto(event);
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

    private EventFullDto toFullDto(Event event) {
        Map<Long, Long> confirmedMap = getConfirmedRequests(List.of(event));
        Map<Long, Long> viewsMap = getViews(List.of(event));
        return EventMapper.toFullDto(
                event,
                confirmedMap.getOrDefault(event.getId(), 0L),
                viewsMap.getOrDefault(event.getId(), 0L)
        );
    }

    private List<EventFullDto> toFullDtos(List<Event> events) {
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, Long> viewsMap = getViews(events);
        return events.stream()
                .map(event -> EventMapper.toFullDto(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private List<EventShortDto> toShortDtos(List<Event> events) {
        Map<Long, Long> confirmedMap = getConfirmedRequests(events);
        Map<Long, Long> viewsMap = getViews(events);
        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .toList();
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
        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .toList();
        String end = LocalDateTime.now().format(FORMATTER);
        List<ViewStatsDto> stats = statsClient.getStats(DEFAULT_START, end, uris, true);
        Map<String, Long> hitsByUri = stats.stream()
                .collect(Collectors.toMap(ViewStatsDto::getUri, ViewStatsDto::getHits));
        Map<Long, Long> result = new HashMap<>();
        for (Event event : events) {
            result.put(event.getId(), hitsByUri.getOrDefault("/events/" + event.getId(), 0L));
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

    private void ensurePagination(int from, int size) {
        if (from < 0 || size <= 0) {
            throw new BadRequestException("Номер страницы должен быть положительный.");
        }
    }

    private void ensureUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден.");
        }
    }
}