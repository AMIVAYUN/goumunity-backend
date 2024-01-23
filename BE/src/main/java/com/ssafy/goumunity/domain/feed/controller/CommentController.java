package com.ssafy.goumunity.domain.feed.controller;

import com.ssafy.goumunity.domain.feed.controller.request.CommentRegistRequest;
import com.ssafy.goumunity.domain.feed.domain.Comment;
import com.ssafy.goumunity.domain.feed.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feeds/{feed-id}/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> saveComment(
            @PathVariable("feed-id") Long id, @RequestBody @Valid CommentRegistRequest comment) {
        return ResponseEntity.ok().body(commentService.saveComment(id, comment));
    }

    @GetMapping
    public ResponseEntity<List<Comment>> findAllComments(@PathVariable("feed-id") Long id) {
        return ResponseEntity.ok(commentService.findAllByFeedId(id));
    }
}
