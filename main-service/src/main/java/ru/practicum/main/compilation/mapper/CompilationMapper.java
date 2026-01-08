package ru.practicum.main.compilation.mapper;

import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;

import java.util.List;
import java.util.Set;

public final class CompilationMapper {
    private CompilationMapper() {
    }

    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        if (compilation == null) {
            return null;
        }

        return CompilationDto.builder()
                .events(events)
                .id(compilation.getId())
                .pinned(compilation.isPinned())
                .title(compilation.getTitle())
                .build();
    }

    public static Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        if (dto == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setPinned(Boolean.TRUE.equals(dto.getPinned()));
        compilation.setTitle(dto.getTitle());
        if (events != null) {
            compilation.setEvents(events);
        }
        return compilation;
    }
}