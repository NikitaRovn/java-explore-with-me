package ru.practicum.stats.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.repository.EndpointHitRepository;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EndpointHitRepository endpointHitRepository;

    @Override
    @Transactional
    public void addHit(EndpointHitDto endpointHitDto) {
        endpointHitRepository.save(StatsMapper.toEntity(endpointHitDto));
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);

        if (startTime.isAfter(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Начало должно быть перед временем окончания.");
        }

        List<String> uriFilter = uris == null ? Collections.emptyList() : uris;
        boolean urisEmpty = uriFilter.isEmpty();

        List<EndpointHitRepository.ViewStatsProjection> stats = unique
                ? endpointHitRepository.findUniqueStats(startTime, endTime, uriFilter, urisEmpty)
                : endpointHitRepository.findStats(startTime, endTime, uriFilter, urisEmpty);

        return stats.stream()
                .map(StatsMapper::toDto)
                .toList();
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не получилось отформатировать дату.", ex);
        }
    }
}