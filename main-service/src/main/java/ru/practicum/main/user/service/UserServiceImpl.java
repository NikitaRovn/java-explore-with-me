package ru.practicum.main.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.model.NewUserRequest;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.mapper.UserMapper;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;

import static ru.practicum.main.utility.Constant.USER_NOT_FOUND;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    @Override
    public UserDto create(NewUserRequest request) {
        User user = UserMapper.toEntity(request);
        try {
            return UserMapper.toDto(repository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email должен быть уникальный.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        if (from < 0 || size <= 0) {
            throw new BadRequestException("Номер страницы должен быть положительный.");
        }
        int page = from / size;
        if (ids == null || ids.isEmpty()) {
            return repository.findAll(PageRequest.of(page, size, Sort.by("id")))
                    .map(UserMapper::toDto)
                    .toList();
        }
        return repository.findAllById(ids).stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Override
    public void delete(long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException(String.format(USER_NOT_FOUND, id));
        }
        repository.deleteById(id);
    }
}