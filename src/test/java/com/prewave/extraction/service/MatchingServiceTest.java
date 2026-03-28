package com.prewave.extraction.service;

import com.prewave.extraction.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingServiceTest {
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService();
    }

    @Test
    void keepOrderTrue_consecutiveTerms_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "de", "Wolfgang Lemb ig metall Germany");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next().queryTermId()).isEqualTo(1);
    }

    @Test
    void keepOrderTrue_outOfOrder_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "de", "Metall something IG");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void keepOrderFalse_scatteredTerms_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(2, "climate change", "en", false));
        Alert alert = alert("a1", "en", "The change in global climate is alarming");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void keepOrderFalse_missingPart_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(2, "climate change", "en", false));
        Alert alert = alert("a1", "en", "The weather is changing dramatically");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void caseInsensitive_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "de", "IG METALL workers unite");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void punctuationHandling_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "de", "Workers of ig metall, unite!");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void hashtagStripping_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(3, "StrikeForBlackLives", "en", true));
        Alert alert = alert("a1", "en", "Support #StrikeForBlackLives today");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void strictLanguage_differentLanguages_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "en", "ig metall workers");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void nonStrictLanguage_differentLanguages_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "en", "ig metall workers");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), false);

        assertThat(results).hasSize(1);
    }

    @Test
    void multipleContents_matchInSecond() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        AlertContent content1 = new AlertContent("Nothing here", "text", "de");
        AlertContent content2 = new AlertContent("ig metall rocks", "text", "de");
        Alert alert = new Alert("a1", List.of(content1, content2), "2020-01-01T00:00:00Z", "tweet");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next().matchedInContent()).isEqualTo("ig metall rocks");
    }

    @Test
    void deduplication_sameAlertAndTerm_onlyOnce() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        AlertContent content1 = new AlertContent("ig metall first", "text", "de");
        AlertContent content2 = new AlertContent("ig metall second", "text", "de");
        Alert alert = new Alert("a1", List.of(content1, content2), "2020-01-01T00:00:00Z", "tweet");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void emptyText_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        Alert alert = alert("a1", "de", "");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void emptyTerms_noMatch() {
        Alert alert = alert("a1", "de", "ig metall workers");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(), true);

        assertThat(results).isEmpty();
    }

    @Test
    void unicodeUmlauts_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(202, "Arbeitsplätze", "de", true));
        Alert alert = alert("a1", "de", "Viele Arbeitsplätze sind betroffen");

        Set<MatchResult> results = matchingService.findMatches(List.of(alert), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    private PreparedQueryTerm prepare(QueryTerm term) {
        return new PreparedQueryTerm(term, matchingService.tokenize(term.text()));
    }

    private Alert alert(String id, String language, String text) {
        AlertContent content = new AlertContent(text, "text", language);
        return new Alert(id, List.of(content), "2020-01-01T00:00:00Z", "tweet");
    }
}
