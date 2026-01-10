package ru.practicum.main.user.service;

import ru.practicum.main.user.model.NewUserRequest;
import ru.practicum.main.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void delete(long id);
}