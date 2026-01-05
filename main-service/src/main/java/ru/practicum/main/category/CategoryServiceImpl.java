package ru.practicum.main.category;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;

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
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена."));
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
            throw new NotFoundException("Категория с id=" + id + " не найдена.");
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(long id) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + id + " не найдена."));
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