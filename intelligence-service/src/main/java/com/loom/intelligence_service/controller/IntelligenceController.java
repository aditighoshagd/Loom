package com.loom.intelligence_service.controller;

import com.loom.intelligence_service.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    @PostMapping("/summarize")
    public ResponseEntity<String> summarizeNewsletter(@RequestBody String content) {
        String summary = intelligenceService.generateSummary(content);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/suggest-tags")
    public ResponseEntity<List<String>> suggestTags(@RequestBody String content) {
        List<String> tags = intelligenceService.suggestTags(content);
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/suggest-title-subtitle")
    public ResponseEntity<String> suggestTitleAndSubtitle(@RequestBody String content) {
        String result = intelligenceService.suggestTitleAndSubtitle(content);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/semantic-search")
    public ResponseEntity<List<Long>> searchSimilarPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        List<Long> matches = intelligenceService.searchSimilarPosts(query, limit);
        return ResponseEntity.ok(matches);
    }
}
