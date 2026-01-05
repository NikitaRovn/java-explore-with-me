package ru.practicum.main.user;

public final class UserMapper {
    private UserMapper() {
    }

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }

    public static User toEntity(NewUserRequest request) {
        if (request == null) {
            return null;
        }
        return new User(null, request.getName(), request.getEmail());
    }
}