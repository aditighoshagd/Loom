package com.loom.intelligence_service.consumer;

import com.loom.intelligence_service.service.IntelligenceService;
import com.loom.postsService.event.PostCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostsConsumer {

    private final IntelligenceService intelligenceService;

    @KafkaListener(topics = "post_created_topic")
    public void handlePostCreated(PostCreated postCreated) {
        log.info("Received post created event: {}", postCreated);
        if (postCreated.getContent() != null && !postCreated.getContent().trim().isEmpty()) {
            try {
                intelligenceService.addPostEmbedding(
                        postCreated.getPostId(),
                        postCreated.getContent(),
                        postCreated.getOwnerUserId()
                );
            } catch (Exception e) {
                log.error("Failed to generate and store embedding for post ID: {}", postCreated.getPostId(), e);
            }
        }
    }
}
