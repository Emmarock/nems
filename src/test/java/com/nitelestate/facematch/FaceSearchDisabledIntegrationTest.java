package com.nitelestate.facematch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With nitel.face-match.enabled off (the default), face search is hidden: the status check says
 * so to any signed-in user (the frontend hides the menu item, page and CCTV wording), and the
 * search endpoint answers 404 as if it didn't exist.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FaceSearchDisabledIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nitel_estate_test")
            .withUsername("nitel")
            .withPassword("nitel");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    private String login(String email, String password) {
        var response = restTemplate.postForEntity(baseUrl() + "/auth/login",
                Map.of("identifier", email, "password", password), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("token");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void switchedOff_statusIsFalseAndSearchIsNotFound() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");

        for (String token : new String[] {adminToken, residentToken}) {
            var status = restTemplate.exchange(baseUrl() + "/face-search/status", org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)), Map.class);
            assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(status.getBody().get("enabled")).isEqualTo(false);
        }

        var search = restTemplate.postForEntity(baseUrl() + "/face-search",
                new HttpEntity<>(Map.of("image", "data:image/jpeg;base64,AAAA"), authHeaders(adminToken)), Map.class);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Still admin-only regardless of the switch.
        var residentSearch = restTemplate.postForEntity(baseUrl() + "/face-search",
                new HttpEntity<>(Map.of("image", "data:image/jpeg;base64,AAAA"), authHeaders(residentToken)), Map.class);
        assertThat(residentSearch.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
