package ru.practicum.main.utility;

import java.time.format.DateTimeFormatter;

public final class Constant {
    private Constant() {
    }

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String NOT_INITIATOR = "Пользователь не является инициатором события.";
}
