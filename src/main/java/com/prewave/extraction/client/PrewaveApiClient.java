package com.prewave.extraction.client;

import com.prewave.extraction.config.PrewaveApiProperties;
import com.prewave.extraction.model.Alert;
import com.prewave.extraction.model.QueryTerm;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Prewave client to fetch data from testQueryTerm and testAlerts endpoints. To be honest I would not put API key
 * as a parameter in a query due to security concerns, instead, better to put it in the headers, but I'm pretty sure
 * it's designed in this way for possible discussions.
 */

@Component
public class PrewaveApiClient {
    private final RestClient restClient;
    private final PrewaveApiProperties properties;

    public PrewaveApiClient(RestClient restClient, PrewaveApiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<QueryTerm> fetchQueryTerms() {
        return restClient.get()
                .uri("/adminInterface/api/testQueryTerm?key={key}", properties.key())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public List<Alert> fetchAlerts() {
        return restClient.get()
                .uri("/adminInterface/api/testAlerts?key={key}", properties.key())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
