package ru.practicum.main.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Collections;

import static ru.practicum.main.utility.Constant.FORMATTER;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return buildResponse(exception.getMessage(), "Запрашиваемый объект не найден.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return buildResponse(exception.getMessage(), "Конфликт.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException exception) {
        return buildResponse(exception.getMessage(), "Неверный запрос.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        return buildResponse("Конфликт.", "Конфликт.",
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "Поле: " + error.getField() + ". Ошибка: " + error.getDefaultMessage())
                .orElse("Ошибка валидации.");
        return buildResponse(message, "Некорректный запрос.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingRequestParam(MissingServletRequestParameterException exception) {
        String message = "Отсутствует обязательный параметр: " + exception.getParameterName();
        return buildResponse(message, "Некорректный запрос.", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception exception) {
        return buildResponse(exception.getMessage(), "Внутренняя ошибка.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ApiError> buildResponse(String message, String reason, HttpStatus status) {
        ApiError apiError = new ApiError(
                Collections.emptyList(),
                message,
                reason,
                status.name(),
                LocalDateTime.now().format(FORMATTER)
        );
        return new ResponseEntity<>(apiError, status);
    }
}