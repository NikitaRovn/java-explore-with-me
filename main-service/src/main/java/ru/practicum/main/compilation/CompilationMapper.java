package ru.practicum.main.compilation;

import ru.practicum.main.event.EventShortDto;

import java.util.List;

public final class CompilationMapper {
    private CompilationMapper() {
    }

    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        if (compilation == null) {
            return null;
        }
        return new CompilationDto(events, compilation.getId(), compilation.isPinned(), compilation.getTitle());
    }
}