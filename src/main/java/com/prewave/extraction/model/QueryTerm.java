package com.prewave.extraction.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QueryTerm(
        int id,
        String text,
        String language,
        @JsonProperty("keepOrder") boolean keepOrder
) {}
