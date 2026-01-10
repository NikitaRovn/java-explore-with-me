package ru.practicum.main.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.comment.enums.CommentStatus;
import ru.practicum.main.comment.model.Comment;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByAuthorId(long authorId, Pageable pageable);

    Optional<Comment> findByIdAndAuthorId(long id, long authorId);

    Page<Comment> findByEventIdAndStatus(long eventId, CommentStatus status, Pageable pageable);

    Page<Comment> findByEventId(long eventId, Pageable pageable);

    Page<Comment> findByStatus(CommentStatus status, Pageable pageable);
}