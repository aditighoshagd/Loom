package com.loom.postsService.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreated {
    private Long ownerUserId;
    private Long postId;
    private Long userId;
    private String content;
    private String title;
}
