package com.prewave.extraction.model;

public record MatchResult(
        String alertId,
        int queryTermId,
        String queryTermText,
        String matchedInContent,
        String contentLanguage
) {}
