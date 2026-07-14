package com.loom.postsService.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDto {
    private Long id;
    private String content;
    private String title;
    private String subTitle;
    private Long parentPostId;
    private Long userId;
    private LocalDateTime createdAt;
}
