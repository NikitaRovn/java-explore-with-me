package ru.practicum.main.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.mapper.CategoryMapper;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private static final String CATEGORY_NOT_FOUND = "Категория с id=%d не найдена.";

    private final CategoryRepository repository;
    private final EventRepository eventRepository;

    @Override
    public CategoryDto create(NewCategoryDto dto) {
        Category category = CategoryMapper.toEntity(dto);
        return CategoryMapper.toDto(repository.save(category));
    }

    @Override
    public CategoryDto update(long id, CategoryDto dto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND, id)));
        category.setName(dto.getName());
        return CategoryMapper.toDto(repository.save(category));
    }

    @Override
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(String.format(CATEGORY_NOT_FOUND, id));
        }

        if (eventRepository.existsByCategoryId(id)) {
            throw new ConflictException("В категории есть события.");
        }

        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND, id)));
        return CategoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        int page = from / size;
        return repository.findAll(PageRequest.of(page, size, Sort.by("id")))
                .map(CategoryMapper::toDto)
                .toList();
    }
}