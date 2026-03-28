package com.prewave.extraction.model;

public record AlertContent(
        String text,
        String type,
        String language
) {}
