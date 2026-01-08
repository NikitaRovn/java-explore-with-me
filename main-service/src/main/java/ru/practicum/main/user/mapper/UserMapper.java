package ru.practicum.main.user.mapper;

import ru.practicum.main.user.model.NewUserRequest;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.dto.UserShortDto;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }

    public static UserShortDto toShortDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserShortDto(user.getId(), user.getName());
    }

    public static User toEntity(NewUserRequest request) {
        if (request == null) {
            return null;
        }
        return new User(null, request.getName(), request.getEmail());
    }
}