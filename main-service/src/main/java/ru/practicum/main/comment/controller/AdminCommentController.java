package ru.practicum.main.comment.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.UpdateCommentStatusRequest;
import ru.practicum.main.comment.enums.CommentStatus;
import ru.practicum.main.comment.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentDto> getComments(@RequestParam(required = false) Long eventId,
                                        @RequestParam(required = false) CommentStatus status,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        return commentService.getAdminComments(eventId, status, from, size);
    }

    @PatchMapping("/{commentId}")
    public CommentDto moderateComment(@PathVariable long commentId,
                                      @Valid @RequestBody UpdateCommentStatusRequest request) {
        return commentService.moderateComment(commentId, request);
    }
}