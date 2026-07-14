package com.loom.postsService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "intelligence-service", path = "/intelligence")
public interface IntelligenceServiceClient {

    @PostMapping("/summarize")
    ResponseEntity<String> summarizeNewsletter(@RequestBody String content);

    @PostMapping("/suggest-tags")
    ResponseEntity<List<String>> suggestTags(@RequestBody String content);

    @PostMapping("/suggest-title-subtitle")
    ResponseEntity<String> suggestTitleAndSubtitle(@RequestBody String content);

    @GetMapping("/semantic-search")
    ResponseEntity<List<Long>> searchSimilarPosts(
            @RequestParam("query") String query,
            @RequestParam("limit") int limit
    );
}
