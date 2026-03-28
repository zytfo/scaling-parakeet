package com.prewave.extraction.controller;

import com.prewave.extraction.client.PrewaveApiClient;
import com.prewave.extraction.model.*;
import com.prewave.extraction.service.MatchingService;
import com.prewave.extraction.service.QueryTermCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchingController.class)
class MatchingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrewaveApiClient apiClient;

    @MockitoBean
    private MatchingService matchingService;

    @MockitoBean
    private QueryTermCacheService cacheService;

    @Test
    void getMatches_returns200WithCorrectStructure() throws Exception {
        List<PreparedQueryTerm> terms = List.of(
                new PreparedQueryTerm(new QueryTerm(1, "IG Metall", "de", true), List.of("ig", "metall"))
        );
        List<Alert> alerts = List.of(new Alert("a1", List.of(new AlertContent("ig metall", "text", "de")), "2020-01-01T00:00:00Z", "tweet"));
        Set<MatchResult> matchResults = new LinkedHashSet<>();
        matchResults.add(new MatchResult("a1", 1, "IG Metall", "ig metall", "de"));

        when(cacheService.getPreparedQueryTerms()).thenReturn(terms);
        when(apiClient.fetchAlerts()).thenReturn(alerts);
        when(matchingService.findMatches(eq(alerts), eq(terms), eq(true))).thenReturn(matchResults);

        mockMvc.perform(get("/api/v1/matches").param("batches", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMatches").value(1))
                .andExpect(jsonPath("$.alertsFetched").value(1))
                .andExpect(jsonPath("$.queryTermsUsed").value(1))
                .andExpect(jsonPath("$.strictLanguageMatch").value(true))
                .andExpect(jsonPath("$.matches[0].alertId").value("a1"))
                .andExpect(jsonPath("$.matches[0].queryTermId").value(1))
                .andExpect(jsonPath("$.matches[0].queryTermText").value("IG Metall"));
    }

    @Test
    void getMatches_strictLanguageFalse_passesParameter() throws Exception {
        when(cacheService.getPreparedQueryTerms()).thenReturn(List.of());
        when(apiClient.fetchAlerts()).thenReturn(List.of());
        when(matchingService.findMatches(any(), any(), eq(false))).thenReturn(new LinkedHashSet<>());

        mockMvc.perform(get("/api/v1/matches").param("strictLanguage", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strictLanguageMatch").value(false));
    }

    @Test
    void getMatches_multipleBatches_aggregatesResults() throws Exception {
        List<PreparedQueryTerm> terms = List.of(
                new PreparedQueryTerm(new QueryTerm(1, "test", "en", false), List.of("test"))
        );
        List<Alert> alerts = List.of(new Alert("a1", List.of(new AlertContent("test content", "text", "en")), "2020-01-01T00:00:00Z", "tweet"));
        Set<MatchResult> matchResults = new LinkedHashSet<>();
        matchResults.add(new MatchResult("a1", 1, "test", "test content", "en"));

        when(cacheService.getPreparedQueryTerms()).thenReturn(terms);
        when(apiClient.fetchAlerts()).thenReturn(alerts);
        when(matchingService.findMatches(eq(alerts), eq(terms), eq(true))).thenReturn(matchResults);

        mockMvc.perform(get("/api/v1/matches").param("batches", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertsFetched").value(3));
    }

    @Test
    void getMatches_batchesExceedsMax_throwsConstraintViolation() {
        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/v1/matches").param("batches", "1001"))
        ).rootCause().isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }

    @Test
    void getMatches_batchesZero_throwsConstraintViolation() {
        assertThatThrownBy(() ->
                mockMvc.perform(get("/api/v1/matches").param("batches", "0"))
        ).rootCause().isInstanceOf(jakarta.validation.ConstraintViolationException.class);
    }

    @Test
    void getQueryTerms_returnsCachedTerms() throws Exception {
        List<QueryTerm> terms = List.of(
                new QueryTerm(1, "IG Metall", "de", true),
                new QueryTerm(2, "climate", "en", false)
        );
        when(cacheService.getQueryTerms()).thenReturn(terms);

        mockMvc.perform(get("/api/v1/query-terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].text").value("climate"));
    }
}
