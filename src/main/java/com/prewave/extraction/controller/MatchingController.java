package com.prewave.extraction.controller;

import com.prewave.extraction.client.PrewaveApiClient;
import com.prewave.extraction.model.Alert;
import com.prewave.extraction.model.MatchResponse;
import com.prewave.extraction.model.MatchResult;
import com.prewave.extraction.model.PreparedQueryTerm;
import com.prewave.extraction.model.QueryTerm;
import com.prewave.extraction.service.MatchingService;
import com.prewave.extraction.service.QueryTermCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@Validated
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Matching")
public class MatchingController {
    private final PrewaveApiClient apiClient;
    private final MatchingService matchingService;
    private final QueryTermCacheService cacheService;

    public MatchingController(
            PrewaveApiClient apiClient, MatchingService matchingService, QueryTermCacheService cacheService
    ) {
        this.apiClient = apiClient;
        this.matchingService = matchingService;
        this.cacheService = cacheService;
    }

    @GetMapping("/query-terms")
    @Operation(summary = "Get cached in-memory query terms")
    public List<QueryTerm> getQueryTerms() {
        return cacheService.getQueryTerms();
    }

    @GetMapping("/matches")
    @Operation(summary = "Find matches across alert batches (100 by default)")
    public MatchResponse getMatches(
            @Parameter(description = "Number of batches to fetch (1-1000)")
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int batches,
            @Parameter(description = "If true match terms with the same language")
            @RequestParam(defaultValue = "true") boolean strictLanguage
    ) {
        List<PreparedQueryTerm> terms = cacheService.getPreparedQueryTerms();
        Set<MatchResult> allResults = new LinkedHashSet<>();

        int totalAlerts = 0;

        // Possibly would require a timeout between requests, but the task says there are no limits
        for (int i = 0; i < batches; i++) {
            List<Alert> alerts = apiClient.fetchAlerts();
            totalAlerts += alerts.size();
            allResults.addAll(matchingService.findMatches(alerts, terms, strictLanguage));
        }

        return new MatchResponse(
                new ArrayList<>(allResults),
                allResults.size(),
                totalAlerts,
                terms.size(),
                strictLanguage
        );
    }
}
