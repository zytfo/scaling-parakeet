package com.prewave.extraction;

import com.prewave.extraction.client.PrewaveApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AlertTermExtractionApplicationTests {
	@MockitoBean
	private PrewaveApiClient apiClient;

	@Test
	void contextLoads() {
	}

}
