package ru.practicum.main.category;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.EventRepository;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private static final String CATEGORY_NOT_FOUND = "Категория с id=%d не найдена.";

    private final CategoryRepository repository;
    private final EventRepository eventRepository;

    @Override
    public CategoryDto create(NewCategoryDto dto) {
        Category category = CategoryMapper.toEntity(dto);
        try {
            return CategoryMapper.toDto(repository.save(category));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Имя категории должно быть уникальным.");
        }
    }

    @Override
    public CategoryDto update(long id, CategoryDto dto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format(CATEGORY_NOT_FOUND, id)));
        category.setName(dto.getName());
        try {
            return CategoryMapper.toDto(repository.save(category));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Имя категории должно быть уникальным.");
        }
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
        if (from < 0 || size <= 0) {
            throw new BadRequestException("Номер страницы должен быть положительный.");
        }
        int page = from / size;
        return repository.findAll(PageRequest.of(page, size, Sort.by("id")))
                .map(CategoryMapper::toDto)
                .toList();
    }
}