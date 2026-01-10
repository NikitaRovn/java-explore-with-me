package ru.practicum.main.comment.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.comment.dto.UpdateCommentDto;
import ru.practicum.main.comment.dto.UpdateCommentStatusRequest;
import ru.practicum.main.comment.enums.CommentStatus;
import ru.practicum.main.comment.mapper.CommentMapper;
import ru.practicum.main.comment.model.Comment;
import ru.practicum.main.comment.repository.CommentRepository;
import ru.practicum.main.event.enums.EventState;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.main.utility.Constant.COMMENT_NOT_FOUND;
import static ru.practicum.main.utility.Constant.EVENT_NOT_FOUND;
import static ru.practicum.main.utility.Constant.USER_NOT_FOUND;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                              UserRepository userRepository,
                              EventRepository eventRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public CommentDto addComment(long userId, long eventId, NewCommentDto dto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format(USER_NOT_FOUND, userId)));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format(EVENT_NOT_FOUND, eventId)));
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя оставлять комментарии к неопубликованным событиям.");
        }
        Comment saved = commentRepository.save(CommentMapper.toEntity(dto, event, author));
        return CommentMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(long userId, int from, int size) {
        ensureUserExists(userId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        return commentRepository.findByAuthorId(userId, pageable)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    public CommentDto updateUserComment(long userId, long commentId, UpdateCommentDto dto) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException(String.format(COMMENT_NOT_FOUND, commentId)));
        if (comment.getStatus() == CommentStatus.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованный комментарий.");
        }
        comment.setText(dto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    public void deleteUserComment(long userId, long commentId) {
        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException(String.format(COMMENT_NOT_FOUND, commentId)));
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getEventComments(long eventId, int from, int size) {
        ensureEventExists(eventId);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        return commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getAdminComments(Long eventId, CommentStatus status, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdOn").descending());
        if (eventId != null && status != null) {
            ensureEventExists(eventId);
            return commentRepository.findByEventIdAndStatus(eventId, status, pageable)
                    .stream()
                    .map(CommentMapper::toDto)
                    .toList();
        }
        if (eventId != null) {
            ensureEventExists(eventId);
            return commentRepository.findByEventId(eventId, pageable)
                    .stream()
                    .map(CommentMapper::toDto)
                    .toList();
        }
        if (status != null) {
            return commentRepository.findByStatus(status, pageable)
                    .stream()
                    .map(CommentMapper::toDto)
                    .toList();
        }
        return commentRepository.findAll(pageable)
                .stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    public CommentDto moderateComment(long commentId, UpdateCommentStatusRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format(COMMENT_NOT_FOUND, commentId)));
        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Модерировать можно только комментарии со статусом PENDING.");
        }
        if (request.getStatus() == CommentStatus.PENDING) {
            throw new ConflictException("Нельзя установить статус PENDING при модерации.");
        }
        comment.setStatus(request.getStatus());
        comment.setUpdatedOn(LocalDateTime.now());
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    private void ensureUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format(USER_NOT_FOUND, userId));
        }
    }

    private void ensureEventExists(long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(String.format(EVENT_NOT_FOUND, eventId));
        }
    }
}