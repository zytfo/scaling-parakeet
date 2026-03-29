package com.prewave.extraction.service;

import com.prewave.extraction.client.PrewaveApiClient;
import com.prewave.extraction.model.PreparedQueryTerm;
import com.prewave.extraction.model.QueryTerm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * As the /query-terms response not that big and it's always deterministic, it makes sense to load data
 * at startup and just cache it in-memory. It also would be applicable to cache this data in, for instance, Redis
 * in case remote API is not available (otherwise the app would not start) and read already fetched terms not making
 * additional calls to API to reduce the load and latency. Query terms are also tokenized at startup to
 * avoid re-tokenizing on every match operation. Terms are grouped by language.
 */

@Service
public class QueryTermCacheService {
    private static final Logger log = LoggerFactory.getLogger(QueryTermCacheService.class);

    private final PrewaveApiClient apiClient;
    private final MatchingService matchingService;

    private List<QueryTerm> cachedTerms = Collections.emptyList();
    private List<PreparedQueryTerm> preparedTerms = Collections.emptyList();
    private Map<String, List<PreparedQueryTerm>> termsByLanguage = Collections.emptyMap();

    public QueryTermCacheService(PrewaveApiClient apiClient, MatchingService matchingService) {
        this.apiClient = apiClient;
        this.matchingService = matchingService;
    }

    @PostConstruct
    public void loadTerms() {
        log.info("Fetching query terms from remote API");
        cachedTerms = List.copyOf(apiClient.fetchQueryTerms());
        preparedTerms = cachedTerms.stream()
                .map(term -> new PreparedQueryTerm(term, matchingService.tokenize(term.text())))
                .toList();
        termsByLanguage = preparedTerms.stream()
                .collect(Collectors.groupingBy(
                        pt -> pt.term().language().toLowerCase(),
                        Collectors.toUnmodifiableList()
                ));
        log.info("Loaded and pre-tokenized {} query terms across {} languages",
                cachedTerms.size(), termsByLanguage.size());
    }

    public List<QueryTerm> getQueryTerms() {
        return cachedTerms;
    }

    public List<PreparedQueryTerm> getPreparedQueryTerms() {
        return preparedTerms;
    }

    public Map<String, List<PreparedQueryTerm>> getTermsByLanguage() {
        return termsByLanguage;
    }
}
