package ru.practicum.stats.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.repository.EndpointHitRepository;

@Service
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EndpointHitRepository endpointHitRepository;

    public StatsServiceImpl(EndpointHitRepository endpointHitRepository) {
        this.endpointHitRepository = endpointHitRepository;
    }

    @Override
    @Transactional
    public void addHit(EndpointHitDto endpointHitDto) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(endpointHitDto.getApp());
        hit.setUri(endpointHitDto.getUri());
        hit.setIp(endpointHitDto.getIp());
        hit.setTimestamp(parseDateTime(endpointHitDto.getTimestamp()));
        endpointHitRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        List<String> uriFilter = uris == null ? Collections.emptyList() : uris;
        boolean urisEmpty = uriFilter.isEmpty();

        List<EndpointHitRepository.ViewStatsProjection> stats = unique
                ? endpointHitRepository.findUniqueStats(startTime, endTime, uriFilter, urisEmpty)
                : endpointHitRepository.findStats(startTime, endTime, uriFilter, urisEmpty);

        return stats.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ViewStatsDto toDto(EndpointHitRepository.ViewStatsProjection projection) {
        ViewStatsDto dto = new ViewStatsDto();
        dto.setApp(projection.getApp());
        dto.setUri(projection.getUri());
        dto.setHits(projection.getHits());
        return dto;
    }

    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }
}