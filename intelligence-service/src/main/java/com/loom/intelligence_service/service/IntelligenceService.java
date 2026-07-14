package com.loom.intelligence_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligenceService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public String generateSummary(String content) {
        log.info("Generating AI summary for newsletter draft");
        String prompt = "You are a newsletter editing assistant. Summarize the following long-form newsletter article " +
                "into a short, engaging Note for a social feed. Keep the summary under 280 characters and do not include " +
                "any intro or outro text:\n\n" + content;

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public List<String> suggestTags(String content) {
        log.info("Suggesting tags for content");
        String prompt = "Analyze the following text and suggest 3 to 5 relevant keyword categories or tags (e.g., technology, productivity, business). " +
                "Provide ONLY a comma-separated list of lowercase tags without any other text:\n\n" + content;

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (response == null || response.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    public String suggestTitleAndSubtitle(String content) {
        log.info("Suggesting title and subtitle suggestions");
        String prompt = "Based on this text, recommend a compelling Title and Subtitle for a newsletter. " +
                "Format your response strictly as:\nTitle: [suggested title]\nSubtitle: [suggested subtitle]\n\nDraft content:\n" + content;

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    public void addPostEmbedding(Long postId, String content, Long userId) {
        log.info("Adding embedding to vector store for post ID: {}", postId);
        Document document = new Document(
                content,
                Map.of("postId", postId, "userId", userId)
        );
        vectorStore.add(List.of(document));
        log.info("Successfully persisted embedding to PgVectorStore");
    }

    public List<Long> searchSimilarPosts(String query, int limit) {
        log.info("Searching similar posts semantically for query: {}", query);
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.query(query).topK(limit)
        );

        return results.stream()
                .map(doc -> {
                    Object postIdObj = doc.getMetadata().get("postId");
                    if (postIdObj instanceof Number) {
                        return ((Number) postIdObj).longValue();
                    } else if (postIdObj instanceof String) {
                        return Long.parseLong((String) postIdObj);
                    }
                    return null;
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }
}
