package com.nitelestate;

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

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof of the auth flow plus the Worker Module lifecycle: resident requests access
 * (issued immediately, no administrator approval step) -> security checks the worker in ->
 * status flips to ACTIVE and is reflected on the Security Dashboard (spec Phase 3 §1).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkerModuleIntegrationTest {

    /** A valid 1x1 PNG - workers and visitors must be registered with a photo. */
    private static final String TEST_PHOTO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

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
    void workersAndVisitors_cannotBeRegisteredWithoutAPhoto() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");

        Map<String, Object> worker = Map.of(
                "fullName", "No Photo Worker",
                "phone", "+2348011113333",
                "contractorName", "Delta Builders Ltd",
                "workType", "Masonry",
                "workerCategory", "NON_DOMESTIC_STAFF",
                "startDate", LocalDate.now().toString(),
                "expectedEndDate", LocalDate.now().plusMonths(1).toString()
        );
        assertThat(restTemplate.postForEntity(baseUrl() + "/workers",
                new HttpEntity<>(worker, authHeaders(residentToken)), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> notAnImage = new java.util.HashMap<>(worker);
        notAnImage.put("photo", "hello");
        assertThat(restTemplate.postForEntity(baseUrl() + "/workers",
                new HttpEntity<>(notAnImage, authHeaders(residentToken)), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> visitor = Map.of(
                "name", "No Photo Visitor",
                "phone", "+2348011114444",
                "validFrom", java.time.Instant.now().toString(),
                "validUntil", java.time.Instant.now().plusSeconds(3600).toString()
        );
        assertThat(restTemplate.postForEntity(baseUrl() + "/visitors",
                new HttpEntity<>(visitor, authHeaders(residentToken)), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void workerLifecycle_requestCheckIn_reflectsOnSecurityDashboard() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        Map<String, Object> workerRequest = Map.of(
                "fullName", "Musa Ibrahim",
                "phone", "+2348011112222",
                "contractorName", "Delta Builders Ltd",
                "workType", "Construction - foundation",
                "workerCategory", "NON_DOMESTIC_STAFF",
                "startDate", LocalDate.now().toString(),
                "expectedEndDate", LocalDate.now().plusMonths(3).toString(),
                "photo", TEST_PHOTO
        );
        var created = restTemplate.postForEntity(baseUrl() + "/workers",
                new HttpEntity<>(workerRequest, authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("status")).isEqualTo("APPROVED");
        String qrToken = (String) created.getBody().get("qrToken");
        assertThat(qrToken).isNotBlank();

        var checkedIn = restTemplate.exchange(baseUrl() + "/workers/checkin/" + qrToken,
                org.springframework.http.HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(checkedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkedIn.getBody().get("status")).isEqualTo("ACTIVE");

        var dashboard = restTemplate.exchange(baseUrl() + "/security/dashboard",
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) dashboard.getBody().get("workersOnSite")).longValue()).isEqualTo(1L);
    }
}
