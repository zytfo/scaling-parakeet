package com.prewave.extraction.model;

import java.util.List;

public record MatchResponse(
        List<MatchResult> matches,
        int totalMatches,
        int alertsFetched,
        int queryTermsUsed,
        boolean strictLanguageMatch
) {
}
