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

    public Set<MatchResult> findMatches(
            List<Alert> alerts,
            Map<String, List<PreparedQueryTerm>> termsByLanguage,
            List<PreparedQueryTerm> allTerms,
            boolean strictLanguage
    ) {
        Set<MatchResult> results = new LinkedHashSet<>();

        for (Alert alert : alerts) {
            Set<Integer> matchedTermIds = new HashSet<>();

            for (AlertContent content : alert.contents()) {
                List<String> tokens = tokenize(content.text());

                List<PreparedQueryTerm> termsToCheck;
                if (strictLanguage) {
                    termsToCheck = termsByLanguage.getOrDefault(content.language().toLowerCase(), List.of());
                } else {
                    termsToCheck = allTerms;
                }

                Map<String, List<PreparedQueryTerm>> orderedByFirstToken = new HashMap<>();
                List<PreparedQueryTerm> unorderedTerms = new ArrayList<>();

                for (PreparedQueryTerm pt : termsToCheck) {
                    if (pt.tokenizedParts().isEmpty()) {
                        continue;
                    }
                    if (pt.term().keepOrder()) {
                        orderedByFirstToken
                                .computeIfAbsent(pt.tokenizedParts().getFirst(), k -> new ArrayList<>())
                                .add(pt);
                    } else {
                        unorderedTerms.add(pt);
                    }
                }

                // for ordered terms
                for (int i = 0; i < tokens.size(); i++) {
                    List<PreparedQueryTerm> candidates = orderedByFirstToken.get(tokens.get(i));
                    if (candidates == null) {
                        continue;
                    }
                    for (PreparedQueryTerm candidate : candidates) {
                        if (matchedTermIds.contains(candidate.term().id())) {
                            continue;
                        }
                        if (matchesOrderedAtPosition(tokens, candidate.tokenizedParts(), i)) {
                            matchedTermIds.add(candidate.term().id());
                            results.add(new MatchResult(
                                    alert.id(),
                                    candidate.term().id(),
                                    candidate.term().text(),
                                    content.text(),
                                    content.language()));
                        }
                    }
                }

                // for unordered terms
                if (!unorderedTerms.isEmpty()) {
                    Set<String> tokenSet = new HashSet<>(tokens);
                    for (PreparedQueryTerm pt : unorderedTerms) {
                        if (matchedTermIds.contains(pt.term().id())) {
                            continue;
                        }
                        if (matchesUnordered(tokenSet, pt.tokenizedParts())) {
                            matchedTermIds.add(pt.term().id());
                            results.add(new MatchResult(
                                    alert.id(),
                                    pt.term().id(),
                                    pt.term().text(),
                                    content.text(),
                                    content.language()));
                        }
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

    private boolean matchesOrderedAtPosition(List<String> tokens, List<String> termParts, int startPos) {
        if (startPos + termParts.size() > tokens.size()) {
            return false;
        }
        for (int j = 0; j < termParts.size(); j++) {
            if (!tokens.get(startPos + j).equals(termParts.get(j))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesUnordered(Set<String> tokenSet, List<String> termParts) {
        for (String part : termParts) {
            if (!tokenSet.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
