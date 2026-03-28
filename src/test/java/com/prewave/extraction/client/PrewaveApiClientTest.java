package com.prewave.extraction.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.maciejwalkowiak.wiremock.spring.ConfigureWireMock;
import com.maciejwalkowiak.wiremock.spring.EnableWireMock;
import com.maciejwalkowiak.wiremock.spring.InjectWireMock;
import com.prewave.extraction.model.Alert;
import com.prewave.extraction.model.QueryTerm;
import com.prewave.extraction.service.QueryTermCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnableWireMock({
        @ConfigureWireMock(name = "prewave-api", properties = "wiremock.server.url")
})
class PrewaveApiClientTest {
    @Autowired
    private PrewaveApiClient client;

    @MockitoBean
    private QueryTermCacheService queryTermCacheService;

    @InjectWireMock("prewave-api")
    private WireMockServer wireMock;

    @Test
    void fetchQueryTerms_returnsDeserializedList() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/query-terms.json"));

        wireMock.stubFor(get(urlPathEqualTo("/adminInterface/api/testQueryTerm"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson(json)));

        List<QueryTerm> terms = client.fetchQueryTerms();

        assertThat(terms).hasSize(2);
        assertThat(terms.getFirst().id()).isEqualTo(101);
        assertThat(terms.getFirst().text()).isEqualTo("IG Metall");
        assertThat(terms.getFirst().keepOrder()).isTrue();
    }

    @Test
    void fetchAlerts_returnsDeserializedList() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/alerts.json"));

        wireMock.stubFor(get(urlPathEqualTo("/adminInterface/api/testAlerts"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson(json)));

        List<Alert> alerts = client.fetchAlerts();

        assertThat(alerts).hasSize(2);
        assertThat(alerts.getFirst().id()).isEqualTo("alert-001");
        assertThat(alerts.getFirst().contents()).hasSize(1);
        assertThat(alerts.getFirst().contents().getFirst().text()).contains("ig metall");
    }

    @Test
    void fetchAlerts_serverError_throwsException() {
        wireMock.stubFor(get(urlPathEqualTo("/adminInterface/api/testAlerts"))
                .willReturn(serverError()));

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> client.fetchAlerts()
        );
    }
}
