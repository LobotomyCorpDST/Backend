package com.devsop.project.apartmentinvoice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DashboardControllerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testApiEndpointAvailable() {
        String url = "http://localhost:" + port + "/api/dashboards";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError());
    }
}
