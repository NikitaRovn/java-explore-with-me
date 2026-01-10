package ru.practicum.main.utility;

import java.time.format.DateTimeFormatter;

public final class Constant {
    private Constant() {
    }

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String DEFAULT_START = "1970-01-01 00:00:00";

    public static final String NOT_INITIATOR = "Пользователь не является инициатором события.";

    public static final String EVENT_NOT_FOUND = "Событие с id=%d не найдено.";

    public static final String CATEGORY_NOT_FOUND = "Категория с id=%d не найдена.";

    public static final String COMPILATION_NOT_FOUND = "Подборка с id=%d не найдена.";

    public static final String USER_NOT_FOUND = "Пользователь с id=%d не найден.";

    public static final String COMMENT_NOT_FOUND = "Комментарий с id=%d не найден.";
}
