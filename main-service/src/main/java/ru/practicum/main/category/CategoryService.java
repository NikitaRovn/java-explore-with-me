package ru.practicum.main.category;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);

    CategoryDto update(long id, CategoryDto dto);

    void delete(long id);

    CategoryDto getById(long id);

    List<CategoryDto> getAll(int from, int size);
}