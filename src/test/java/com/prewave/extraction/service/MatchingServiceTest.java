package com.prewave.extraction.service;

import com.prewave.extraction.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        Set<MatchResult> results = match(alert("a1", "de", "Wolfgang Lemb ig metall Germany"), List.of(term), true);

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next().queryTermId()).isEqualTo(1);
    }

    @Test
    void keepOrderTrue_outOfOrder_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));

        Set<MatchResult> results = match(alert("a1", "de", "Metall something IG"), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void keepOrderFalse_scatteredTerms_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(2, "climate change", "en", false));

        Set<MatchResult> results = match(alert("a1", "en", "The change in global climate is alarming"), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void keepOrderFalse_missingPart_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(2, "climate change", "en", false));

        Set<MatchResult> results = match(alert("a1", "en", "The weather is changing dramatically"), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void caseInsensitive_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));

        Set<MatchResult> results = match(alert("a1", "de", "IG METALL workers unite"), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void punctuationHandling_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));

        Set<MatchResult> results = match(alert("a1", "de", "Workers of ig metall, unite!"), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void hashtagStripping_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(3, "StrikeForBlackLives", "en", true));

        Set<MatchResult> results = match(alert("a1", "en", "Support #StrikeForBlackLives today"), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void strictLanguage_differentLanguages_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));

        Set<MatchResult> results = match(alert("a1", "en", "ig metall workers"), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void nonStrictLanguage_differentLanguages_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));

        Set<MatchResult> results = match(alert("a1", "en", "ig metall workers"), List.of(term), false);

        assertThat(results).hasSize(1);
    }

    @Test
    void multipleContents_matchInSecond() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        AlertContent content1 = new AlertContent("Nothing here", "text", "de");
        AlertContent content2 = new AlertContent("ig metall rocks", "text", "de");
        Alert alert = new Alert("a1", List.of(content1, content2), "2020-01-01T00:00:00Z", "tweet");

        Set<MatchResult> results = match(alert, List.of(term), true);

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next().matchedInContent()).isEqualTo("ig metall rocks");
    }

    @Test
    void deduplication_sameAlertAndTerm_onlyOnce() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));
        AlertContent content1 = new AlertContent("ig metall first", "text", "de");
        AlertContent content2 = new AlertContent("ig metall second", "text", "de");
        Alert alert = new Alert("a1", List.of(content1, content2), "2020-01-01T00:00:00Z", "tweet");

        Set<MatchResult> results = match(alert, List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void emptyText_noMatch() {
        PreparedQueryTerm term = prepare(new QueryTerm(1, "IG Metall", "de", true));

        Set<MatchResult> results = match(alert("a1", "de", ""), List.of(term), true);

        assertThat(results).isEmpty();
    }

    @Test
    void emptyTerms_noMatch() {
        Set<MatchResult> results = match(alert("a1", "de", "ig metall workers"), List.of(), true);

        assertThat(results).isEmpty();
    }

    @Test
    void unicodeUmlauts_matches() {
        PreparedQueryTerm term = prepare(new QueryTerm(202, "Arbeitsplätze", "de", true));

        Set<MatchResult> results = match(alert("a1", "de", "Viele Arbeitsplätze sind betroffen"), List.of(term), true);

        assertThat(results).hasSize(1);
    }

    @Test
    void languageGrouping_onlyChecksRelevantTerms() {
        PreparedQueryTerm deTerm = prepare(new QueryTerm(1, "IG Metall", "de", true));
        PreparedQueryTerm enTerm = prepare(new QueryTerm(2, "pollution", "en", true));
        Alert alert = alert("a1", "de", "ig metall pollution");

        Set<MatchResult> results = match(alert, List.of(deTerm, enTerm), true);

        assertThat(results).hasSize(1);
        assertThat(results.iterator().next().queryTermId()).isEqualTo(1);
    }

    @Test
    void languageGrouping_nonStrictMatchesBothLanguages() {
        PreparedQueryTerm deTerm = prepare(new QueryTerm(1, "IG Metall", "de", true));
        PreparedQueryTerm enTerm = prepare(new QueryTerm(2, "pollution", "en", true));
        Alert alert = alert("a1", "de", "ig metall pollution");

        Set<MatchResult> results = match(alert, List.of(deTerm, enTerm), false);

        assertThat(results).hasSize(2);
    }

    @Test
    void invertedIndex_multipleOrderedTermsSameFirstToken() {
        PreparedQueryTerm term1 = prepare(new QueryTerm(1, "ig metall", "de", true));
        PreparedQueryTerm term2 = prepare(new QueryTerm(2, "ig bergbau", "de", true));
        Alert alert = alert("a1", "de", "workers ig metall and ig bergbau unite");

        Set<MatchResult> results = match(alert, List.of(term1, term2), true);

        assertThat(results).hasSize(2);
    }

    @Test
    void invertedIndex_mixedOrderedAndUnordered() {
        PreparedQueryTerm ordered = prepare(new QueryTerm(1, "ig metall", "de", true));
        PreparedQueryTerm unordered = prepare(new QueryTerm(2, "workers unite", "de", false));
        Alert alert = alert("a1", "de", "unite ig metall workers");

        Set<MatchResult> results = match(alert, List.of(ordered, unordered), true);

        assertThat(results).hasSize(2);
    }

    private Set<MatchResult> match(Alert alert, List<PreparedQueryTerm> terms, boolean strictLanguage) {
        Map<String, List<PreparedQueryTerm>> byLanguage = terms.stream()
                .collect(Collectors.groupingBy(pt -> pt.term().language().toLowerCase()));
        return matchingService.findMatches(List.of(alert), byLanguage, terms, strictLanguage);
    }

    private PreparedQueryTerm prepare(QueryTerm term) {
        return new PreparedQueryTerm(term, matchingService.tokenize(term.text()));
    }

    private Alert alert(String id, String language, String text) {
        AlertContent content = new AlertContent(text, "text", language);
        return new Alert(id, List.of(content), "2020-01-01T00:00:00Z", "tweet");
    }
}
