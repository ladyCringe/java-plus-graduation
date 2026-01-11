package ru.practicum.event.service;

import ru.practicum.event.dto.CompilationDto;
import ru.practicum.event.dto.FindCompilationParams;
import ru.practicum.event.dto.NewCompilationDto;
import ru.practicum.event.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(UpdateCompilationRequest dto, Long compId);

    void delete(Long compId);

    CompilationDto findById(Long compId);

    List<CompilationDto> getAll(FindCompilationParams params);
}
