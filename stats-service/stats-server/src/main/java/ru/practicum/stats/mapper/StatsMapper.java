package ru.practicum.stats.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.repository.EndpointHitRepository;

public final class StatsMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private StatsMapper() {
    }

    public static EndpointHit toEntity(EndpointHitDto dto) {
        EndpointHit hit = new EndpointHit();
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setTimestamp(LocalDateTime.parse(dto.getTimestamp(), FORMATTER));
        return hit;
    }

    public static ViewStatsDto toDto(EndpointHitRepository.ViewStatsProjection projection) {
        ViewStatsDto dto = new ViewStatsDto();
        dto.setApp(projection.getApp());
        dto.setUri(projection.getUri());
        dto.setHits(projection.getHits());
        return dto;
    }
}