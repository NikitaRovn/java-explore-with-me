package ru.practicum.main.comment.service;

import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.comment.dto.UpdateCommentDto;
import ru.practicum.main.comment.dto.UpdateCommentStatusRequest;
import ru.practicum.main.comment.enums.CommentStatus;

import java.util.List;

public interface CommentService {
    CommentDto addComment(long userId, long eventId, NewCommentDto dto);

    List<CommentDto> getUserComments(long userId, int from, int size);

    CommentDto updateUserComment(long userId, long commentId, UpdateCommentDto dto);

    void deleteUserComment(long userId, long commentId);

    List<CommentDto> getEventComments(long eventId, int from, int size);

    List<CommentDto> getAdminComments(Long eventId, CommentStatus status, int from, int size);

    CommentDto moderateComment(long commentId, UpdateCommentStatusRequest request);
}