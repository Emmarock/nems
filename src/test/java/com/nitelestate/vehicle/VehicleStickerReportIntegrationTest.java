package com.nitelestate.vehicle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The vehicle sticker register PDF: office roles can download it, residents can't, and every
 * active vehicle comes out of it with a working gate pass (one is issued if it had none).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VehicleStickerReportIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void stickerReport_isAPdfAndIssuesMissingPasses() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        var created = restTemplate.postForEntity(baseUrl() + "/me/vehicles",
                new HttpEntity<>(Map.of("plateNumber", "STICK-001"), authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long vehicleId = ((Number) created.getBody().get("id")).longValue();
        assertThat(jdbcTemplate.queryForObject("select qr_token from vehicle where id = ?", String.class, vehicleId)).isNull();

        var report = restTemplate.exchange(baseUrl() + "/vehicles/sticker-report",
                HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), byte[].class);
        assertThat(report.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(report.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(new String(report.getBody(), 0, 5)).isEqualTo("%PDF-");

        // The vehicle now has a pass, so the QR printed on its sticker works at the gate.
        String qr = jdbcTemplate.queryForObject("select qr_token from vehicle where id = ?", String.class, vehicleId);
        assertThat(qr).isNotBlank();
        var lookup = restTemplate.exchange(baseUrl() + "/vehicles/lookup/" + qr,
                HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(lookup.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lookup.getBody().get("plateNumber")).isEqualTo("STICK-001");

        var forbidden = restTemplate.exchange(baseUrl() + "/vehicles/sticker-report",
                HttpMethod.GET, new HttpEntity<>(authHeaders(residentToken)), byte[].class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
