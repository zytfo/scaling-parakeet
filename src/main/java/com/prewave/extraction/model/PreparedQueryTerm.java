package com.prewave.extraction.model;

import java.util.List;

/**
 * Tokenized (at startup) query term.
 */
public record PreparedQueryTerm(
        QueryTerm term,
        List<String> tokenizedParts
) {}
