package com.prewave.extraction.service;

import com.prewave.extraction.client.PrewaveApiClient;
import com.prewave.extraction.model.QueryTerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryTermCacheServiceTest {
    @Mock
    private PrewaveApiClient apiClient;

    @Spy
    private MatchingService matchingService;

    @InjectMocks
    private QueryTermCacheService cacheService;

    @Test
    void loadTerms_fetchesAndCachesTerms() {
        List<QueryTerm> terms = List.of(
                new QueryTerm(1, "IG Metall", "de", true),
                new QueryTerm(2, "climate change", "en", false)
        );
        when(apiClient.fetchQueryTerms()).thenReturn(terms);

        cacheService.loadTerms();

        assertThat(cacheService.getQueryTerms()).hasSize(2);
        assertThat(cacheService.getQueryTerms().getFirst().id()).isEqualTo(1);
    }

    @Test
    void loadTerms_preTokenizesTerms() {
        List<QueryTerm> terms = List.of(
                new QueryTerm(1, "IG Metall", "de", true)
        );
        when(apiClient.fetchQueryTerms()).thenReturn(terms);

        cacheService.loadTerms();

        assertThat(cacheService.getPreparedQueryTerms()).hasSize(1);
        assertThat(cacheService.getPreparedQueryTerms().getFirst().tokenizedParts())
                .containsExactly("ig", "metall");
    }

    @Test
    void loadTerms_groupsTermsByLanguage() {
        List<QueryTerm> terms = List.of(
                new QueryTerm(1, "IG Metall", "de", true),
                new QueryTerm(2, "Arbeitsplatz", "de", true),
                new QueryTerm(3, "pollution", "en", true)
        );
        when(apiClient.fetchQueryTerms()).thenReturn(terms);

        cacheService.loadTerms();

        assertThat(cacheService.getTermsByLanguage()).hasSize(2);
        assertThat(cacheService.getTermsByLanguage().get("de")).hasSize(2);
        assertThat(cacheService.getTermsByLanguage().get("en")).hasSize(1);
    }
}
