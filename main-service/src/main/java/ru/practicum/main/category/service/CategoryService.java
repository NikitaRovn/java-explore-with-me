package ru.practicum.main.category.service;

import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    CategoryDto update(long id, CategoryDto dto);

    void delete(long id);

    CategoryDto getById(long id);

    List<CategoryDto> getAll(int from, int size);
}