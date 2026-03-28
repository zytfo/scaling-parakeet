package com.prewave.extraction.service;

import com.prewave.extraction.model.Alert;
import com.prewave.extraction.model.AlertContent;
import com.prewave.extraction.model.MatchResult;
import com.prewave.extraction.model.PreparedQueryTerm;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Main logic for matching.
 */

@Service
public class MatchingService {
    // splits text on any character that is not a digit or Unicode letter
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[^\\p{L}\\p{N}]+");

    public Set<MatchResult> findMatches(List<Alert> alerts, List<PreparedQueryTerm> queryTerms, boolean strictLanguage) {
        Set<MatchResult> results = new LinkedHashSet<>();

        for (Alert alert : alerts) {
            Set<Integer> matchedTermIds = new HashSet<>();

            for (AlertContent content : alert.contents()) {
                List<String> tokens = tokenize(content.text());

                for (PreparedQueryTerm preparedQueryTerm : queryTerms) {
                    if (matchedTermIds.contains(preparedQueryTerm.term().id())) {
                        continue;
                    }

                    if (strictLanguage && !preparedQueryTerm.term().language().equalsIgnoreCase(content.language())) {
                        continue;
                    }

                    List<String> termParts = preparedQueryTerm.tokenizedParts();

                    boolean matched;
                    if (preparedQueryTerm.term().keepOrder()) {
                        matched = matchesOrdered(tokens, termParts);
                    } else {
                        matched = matchesUnordered(tokens, termParts);
                    }

                    if (matched) {
                        matchedTermIds.add(preparedQueryTerm.term().id());
                        results.add(
                                new MatchResult(
                                    alert.id(),
                                    preparedQueryTerm.term().id(),
                                    preparedQueryTerm.term().text(),
                                    content.text(),
                                    content.language())
                        );
                    }
                }
            }
        }

        return results;
    }

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(SPLIT_PATTERN.split(text.toLowerCase())).filter(s -> !s.isEmpty()).toList();
    }

    private boolean matchesOrdered(List<String> tokens, List<String> termParts) {
        if (termParts.size() > tokens.size()) {
            return false;
        }

        for (int i = 0; i <= tokens.size() - termParts.size(); i++) {
            boolean match = true;
            for (int j = 0; j < termParts.size(); j++) {
                if (!tokens.get(i + j).equals(termParts.get(j))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesUnordered(List<String> tokens, List<String> termParts) {
        Set<String> tokenSet = new HashSet<>(tokens);
        for (String part : termParts) {
            if (!tokenSet.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
