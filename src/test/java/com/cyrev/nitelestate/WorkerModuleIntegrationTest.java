package com.cyrev.nitelestate;

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
 * End-to-end proof of the auth flow plus the Worker Module lifecycle (spec Phase 2 §4):
 * resident requests access -> CDA admin approves -> security checks the worker in ->
 * status flips to ACTIVE and is reflected on the Security Dashboard (spec Phase 3 §1).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkerModuleIntegrationTest {

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
                Map.of("email", email, "password", password), Map.class);
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
    void workerLifecycle_requestApproveCheckIn_reflectsOnSecurityDashboard() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String cdaToken = login("cda@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        Map<String, Object> workerRequest = Map.of(
                "fullName", "Musa Ibrahim",
                "phone", "+2348011112222",
                "contractorName", "Delta Builders Ltd",
                "workType", "Construction - foundation",
                "startDate", LocalDate.now().toString(),
                "expectedEndDate", LocalDate.now().plusMonths(3).toString()
        );
        var created = restTemplate.postForEntity(baseUrl() + "/workers",
                new HttpEntity<>(workerRequest, authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("status")).isEqualTo("PENDING");
        Integer workerId = (Integer) created.getBody().get("id");

        var approved = restTemplate.exchange(baseUrl() + "/workers/" + workerId + "/approve",
                org.springframework.http.HttpMethod.POST, new HttpEntity<>(authHeaders(cdaToken)), Map.class);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody().get("status")).isEqualTo("APPROVED");
        String qrToken = (String) approved.getBody().get("qrToken");
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
