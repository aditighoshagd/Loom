package com.loom.notification_service.consumer;

import com.loom.notification_service.entity.Notification;
import com.loom.notification_service.service.NotificationService;
import com.loom.postsService.event.PostCreated;
import com.loom.postsService.event.PostLiked;
import com.loom.postsService.event.CommentCreatedEvent;
import com.loom.postsService.event.PostRestackedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostsConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "post_created_topic")
    public void handlePostCreated(PostCreated postCreated) {
        log.info("handlePostCreated: {}", postCreated);

        if (postCreated.getTitle() != null && !postCreated.getTitle().trim().isEmpty()) {
            log.info("[NEWSLETTER EMAIL DISPATCH] Sent publication '{}' to subscriber ID {} at {}",
                    postCreated.getTitle(), postCreated.getUserId(), LocalDateTime.now());
        }

        String message = String.format("Writer with id: %d has published: %s",
                postCreated.getOwnerUserId(), postCreated.getContent());
        Notification notification = Notification.builder()
                .message(message)
                .userId(postCreated.getUserId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "post_liked_topic")
    public void handlePostLiked(PostLiked postLiked) {
        log.info("handlePostLiked: {}", postLiked);

        String message = String.format("User with id: %d has liked your post with id: %d",
                postLiked.getLikedByUserId(), postLiked.getPostId());

        Notification notification = Notification.builder()
                .message(message)
                .userId(postLiked.getOwnerUserId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "comment_created_topic")
    public void handleCommentCreated(CommentCreatedEvent commentCreatedEvent) {
        log.info("handleCommentCreated: {}", commentCreatedEvent);

        String message = String.format("User with id: %d commented on your post: %s",
                commentCreatedEvent.getCommenterUserId(), commentCreatedEvent.getContent());

        Notification notification = Notification.builder()
                .message(message)
                .userId(commentCreatedEvent.getPostOwnerUserId())
                .build();
        notificationService.addNotification(notification);
    }

    @KafkaListener(topics = "post_restacked_topic")
    public void handlePostRestacked(PostRestackedEvent postRestackedEvent) {
        log.info("handlePostRestacked: {}", postRestackedEvent);

        String message = String.format("User with id: %d restacked your post",
                postRestackedEvent.getRestackedByUserId());

        Notification notification = Notification.builder()
                .message(message)
                .userId(postRestackedEvent.getOwnerUserId())
                .build();
        notificationService.addNotification(notification);
    }
}












