package com.prewave.extraction.model;

import java.util.List;

public record Alert(
        String id,
        List<AlertContent> contents,
        String date,
        String inputType
) {}
