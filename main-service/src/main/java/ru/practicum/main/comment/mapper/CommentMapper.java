package ru.practicum.main.comment.mapper;

import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.comment.enums.CommentStatus;
import ru.practicum.main.comment.model.Comment;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.user.model.User;

import java.time.LocalDateTime;

public final class CommentMapper {
    private CommentMapper() {
    }

    public static Comment toEntity(NewCommentDto dto, Event event, User author) {
        if (dto == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        return Comment.builder()
                .text(dto.getText())
                .event(event)
                .author(author)
                .createdOn(now)
                .updatedOn(now)
                .status(CommentStatus.PENDING)
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthor().getId())
                .eventId(comment.getEvent().getId())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .status(comment.getStatus())
                .build();
    }
}