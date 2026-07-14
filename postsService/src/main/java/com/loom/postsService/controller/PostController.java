package com.loom.postsService.controller;

import com.loom.postsService.auth.AuthContextHolder;
import com.loom.postsService.dto.PostCreateRequestDto;
import com.loom.postsService.dto.PostDto;
import com.loom.postsService.dto.CommentDto;
import com.loom.postsService.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core")
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDto> createPost(@RequestPart("post") PostCreateRequestDto postCreateRequestDto,
                                              @RequestPart(value = "file", required = false) MultipartFile file) {
        PostDto postDto = postService.createPost(postCreateRequestDto, file);
        return new ResponseEntity<>(postDto, HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long postId) {
        PostDto postDto = postService.getPostById(postId);
        return ResponseEntity.ok(postDto);
    }

    @GetMapping("/users/{userId}/allPosts")
    public ResponseEntity<List<PostDto>> getAllPostsOfUser(@PathVariable Long userId) {
        List<PostDto> posts = postService.getAllPostsOfUser(userId);
        return ResponseEntity.ok(posts);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentDto> addComment(@PathVariable Long postId, @RequestBody String content) {
        CommentDto commentDto = postService.addComment(postId, content);
        return new ResponseEntity<>(commentDto, HttpStatus.CREATED);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsForPost(@PathVariable Long postId) {
        List<CommentDto> comments = postService.getCommentsForPost(postId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<PostDto>> getInboxFeed() {
        List<PostDto> feed = postService.getFeedForUser();
        return ResponseEntity.ok(feed);
    }

    @GetMapping("/explore")
    public ResponseEntity<List<PostDto>> getExploreFeed() {
        List<PostDto> explore = postService.getGlobalExploreFeed();
        return ResponseEntity.ok(explore);
    }

    @PostMapping("/{postId}/restack")
    public ResponseEntity<PostDto> restackPost(@PathVariable Long postId, @RequestBody(required = false) String content) {
        PostDto restack = postService.restackPost(postId, content);
        return new ResponseEntity<>(restack, HttpStatus.CREATED);
    }
}
