package com.loom.postsService.dto;

import lombok.Data;

@Data
public class PostCreateRequestDto {
    private String content;
    private String title;
    private String subTitle;
    private Long parentPostId;
}
