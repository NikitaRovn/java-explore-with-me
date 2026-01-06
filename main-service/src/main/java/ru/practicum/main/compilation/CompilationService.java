package ru.practicum.main.compilation;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    void delete(long compId);

    CompilationDto update(long compId, UpdateCompilationRequest request);

    List<CompilationDto> getCompilations(Boolean pinned, int from, int size);

    CompilationDto getCompilation(long compId);
}