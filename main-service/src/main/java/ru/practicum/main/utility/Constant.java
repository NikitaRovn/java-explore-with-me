package ru.practicum.main.utility;

import java.time.format.DateTimeFormatter;

public final class Constant {
    private Constant() {
    }

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
