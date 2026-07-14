package com.loom.postsService.service;

import com.loom.postsService.auth.AuthContextHolder;
import com.loom.postsService.client.ConnectionsServiceClient;
import com.loom.postsService.client.UploaderServiceClient;
import com.loom.postsService.client.IntelligenceServiceClient;
import com.loom.postsService.dto.PersonDto;
import com.loom.postsService.dto.PostCreateRequestDto;
import com.loom.postsService.dto.PostDto;
import com.loom.postsService.dto.CommentDto;
import com.loom.postsService.entity.Post;
import com.loom.postsService.entity.Comment;
import com.loom.postsService.event.PostCreated;
import com.loom.postsService.event.CommentCreatedEvent;
import com.loom.postsService.event.PostRestackedEvent;
import com.loom.postsService.exception.ResourceNotFoundException;
import com.loom.postsService.repository.PostRepository;
import com.loom.postsService.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ModelMapper modelMapper;
    private final ConnectionsServiceClient connectionsServiceClient;
    private final IntelligenceServiceClient intelligenceServiceClient;
    private final KafkaTemplate<Long, PostCreated> postCreatedKafkaTemplate;
    private final KafkaTemplate<Long, CommentCreatedEvent> commentCreatedKafkaTemplate;
    private final KafkaTemplate<Long, PostRestackedEvent> postRestackedKafkaTemplate;
    private final UploaderServiceClient uploaderServiceClient;

    public PostDto createPost(PostCreateRequestDto postCreateRequestDto, MultipartFile file) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Creating post for user with id: {}", userId);

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                ResponseEntity<String> response = uploaderServiceClient.uploadFile(file);
                imageUrl = response.getBody();
            } catch (Exception e) {
                log.error("Failed to upload file, proceeding without image", e);
            }
        }

        Post post = modelMapper.map(postCreateRequestDto, Post.class);
        post.setUserId(userId);
        post.setImageUrl(imageUrl);
        post = postRepository.save(post);

        List<PersonDto> personDtoList = connectionsServiceClient.getFirstDegreeConnections(userId);

        for(PersonDto person: personDtoList) { // send notification to each subscriber
            PostCreated postCreated = PostCreated.builder()
                    .postId(post.getId())
                    .content(post.getContent())
                    .userId(person.getUserId())
                    .ownerUserId(userId)
                    .title(post.getTitle())
                    .build();
            postCreatedKafkaTemplate.send("post_created_topic", postCreated);
        }

        return modelMapper.map(post, PostDto.class);
    }

    public PostDto getPostById(Long postId) {
        log.info("Getting the post with ID: {}", postId);
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found " +
                "with ID: "+postId));
        return modelMapper.map(post, PostDto.class);
    }

    public List<PostDto> getAllPostsOfUser(Long userId) {
        log.info("Getting all the posts of a user with ID: {}", userId);
        List<Post> postList = postRepository.findByUserId(userId);

        return postList
                .stream()
                .map((element) -> modelMapper.map(element, PostDto.class))
                .collect(Collectors.toList());
    }

    public CommentDto addComment(Long postId, String content) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Adding comment to post with ID: {} by user: {}", postId, userId);
        
        Post post = postRepository.findById(postId).orElseThrow(() -> 
            new ResourceNotFoundException("Post not found with ID: " + postId));
        
        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment = commentRepository.save(comment);
        
        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .commentId(comment.getId())
                .postId(postId)
                .commenterUserId(userId)
                .postOwnerUserId(post.getUserId())
                .content(content)
                .build();
        commentCreatedKafkaTemplate.send("comment_created_topic", event);
        
        return modelMapper.map(comment, CommentDto.class);
    }

    public List<CommentDto> getCommentsForPost(Long postId) {
        log.info("Getting comments for post with ID: {}", postId);
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return comments.stream()
                .map(comment -> modelMapper.map(comment, CommentDto.class))
                .collect(Collectors.toList());
    }

    public List<PostDto> getFeedForUser() {
        Long currentUserId = AuthContextHolder.getCurrentUserId();
        log.info("Getting feed for subscriber with ID: {}", currentUserId);
        
        List<PersonDto> writers = connectionsServiceClient.getFirstDegreeConnections(currentUserId);
        List<Long> writerIds = writers.stream()
                .map(PersonDto::getUserId)
                .collect(Collectors.toList());
        
        if (writerIds.isEmpty()) {
            return List.of();
        }
        
        List<Post> posts = postRepository.findByUserIdInOrderByCreatedAtDesc(writerIds);
        return posts.stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }

    public List<PostDto> getGlobalExploreFeed() {
        log.info("Getting global explore feed");
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return posts.stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }

    public PostDto restackPost(Long originalPostId, String noteContent) {
        Long userId = AuthContextHolder.getCurrentUserId();
        log.info("Restacking post with ID: {} by user: {}", originalPostId, userId);
        
        Post originalPost = postRepository.findById(originalPostId).orElseThrow(() -> 
            new ResourceNotFoundException("Original post not found with ID: " + originalPostId));
            
        Post restack = new Post();
        restack.setUserId(userId);
        restack.setContent(noteContent == null || noteContent.trim().isEmpty() ? "Restacked this note" : noteContent);
        restack.setParentPostId(originalPostId);
        restack = postRepository.save(restack);
        
        PostRestackedEvent event = PostRestackedEvent.builder()
                .postId(restack.getId())
                .originalPostId(originalPostId)
                .restackedByUserId(userId)
                .ownerUserId(originalPost.getUserId())
                .build();
        postRestackedKafkaTemplate.send("post_restacked_topic", event);
        
        return modelMapper.map(restack, PostDto.class);
    }

    public String getAiSummary(Long postId) {
        log.info("Requesting AI summary for post: {}", postId);
        Post post = postRepository.findById(postId).orElseThrow(() -> 
            new ResourceNotFoundException("Post not found with ID: " + postId));
        return intelligenceServiceClient.summarizeNewsletter(post.getContent()).getBody();
    }

    public List<String> getAiTags(Long postId) {
        log.info("Requesting AI suggested tags for post: {}", postId);
        Post post = postRepository.findById(postId).orElseThrow(() -> 
            new ResourceNotFoundException("Post not found with ID: " + postId));
        return intelligenceServiceClient.suggestTags(post.getContent()).getBody();
    }

    public List<PostDto> semanticSearch(String query, int limit) {
        log.info("Performing semantic search with query: {}", query);
        List<Long> matchingIds = intelligenceServiceClient.searchSimilarPosts(query, limit).getBody();
        if (matchingIds == null || matchingIds.isEmpty()) {
            return List.of();
        }
        List<Post> posts = postRepository.findAllById(matchingIds);
        return posts.stream()
                .map(post -> modelMapper.map(post, PostDto.class))
                .collect(Collectors.toList());
    }
}
