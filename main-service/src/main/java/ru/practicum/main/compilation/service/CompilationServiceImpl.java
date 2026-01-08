package ru.practicum.main.compilation.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.compilation.mapper.CompilationMapper;
import ru.practicum.main.compilation.repository.CompilationRepository;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.mapper.EventMapper;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.repository.ParticipationRequestRepository;
import ru.practicum.main.request.enums.RequestCount;
import ru.practicum.main.request.enums.RequestStatus;
import ru.practicum.stats.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.practicum.main.utility.Constant.FORMATTER;

@Service
@Transactional
public class CompilationServiceImpl implements CompilationService {
    private static final String DEFAULT_START = "1970-01-01 00:00:00";
    private static final String COMPILATION_NOT_FOUND = "Подборка с id=%d не найдена.";

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    public CompilationServiceImpl(CompilationRepository compilationRepository,
                                  EventRepository eventRepository,
                                  ParticipationRequestRepository requestRepository,
                                  StatsClient statsClient) {
        this.compilationRepository = compilationRepository;
        this.eventRepository = eventRepository;
        this.requestRepository = requestRepository;
        this.statsClient = statsClient;
    }

    @Override
    public CompilationDto create(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setPinned(Boolean.TRUE.equals(dto.getPinned()));
        compilation.setTitle(dto.getTitle());
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            compilation.setEvents(events);
        }
        Compilation saved = compilationRepository.save(compilation);
        return toDto(saved);
    }

    @Override
    public void delete(long compId) {
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException(String.format(COMPILATION_NOT_FOUND, compId));
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public CompilationDto update(long compId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format(COMPILATION_NOT_FOUND, compId)));
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            compilation.setEvents(events);
        }
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        ensurePagination(from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).getContent();
        } else {
            compilations = compilationRepository.findByPinned(pinned, pageable).getContent();
        }
        return compilations.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format(COMPILATION_NOT_FOUND, compId)));
        return toDto(compilation);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Event> events = compilation.getEvents().stream().toList();
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<Long, Long> views = getViews(events);
        List<EventShortDto> eventDtos = events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        confirmed.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
        return CompilationMapper.toDto(compilation, eventDtos);
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
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

    private void ensurePagination(int from, int size) {
        if (from < 0 || size <= 0) {
            throw new BadRequestException("Номер страницы должен быть положительный.");
        }
    }
}