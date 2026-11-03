package com.nitelestate.vehicle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * A deactivated vehicle's QR pass must stop working: check-in refused, pass no longer viewable,
 * check-out still allowed. Re-approving issues a fresh QR so the old one stays dead.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VehicleDeactivationIntegrationTest {

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
    @SuppressWarnings("unchecked")
    void deactivatedVehicle_qrPassStopsWorking() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        var created = restTemplate.postForEntity(baseUrl() + "/me/vehicles",
                new HttpEntity<>(Map.of("plateNumber", "DEACT-001"), authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long vehicleId = ((Number) created.getBody().get("id")).longValue();

        var pass = restTemplate.exchange(baseUrl() + "/me/vehicles/" + vehicleId + "/access-pass",
                HttpMethod.GET, new HttpEntity<>(authHeaders(residentToken)), Map.class);
        assertThat(pass.getStatusCode()).isEqualTo(HttpStatus.OK);
        String qr = (String) pass.getBody().get("qrToken");

        var firstIn = restTemplate.exchange(baseUrl() + "/vehicles/checkin/" + qr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(firstIn.getStatusCode()).isEqualTo(HttpStatus.OK);

        var deactivated = restTemplate.exchange(baseUrl() + "/vehicles/" + vehicleId + "/deactivate",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody().get("status")).isEqualTo("INACTIVE");

        var blockedIn = restTemplate.exchange(baseUrl() + "/vehicles/checkin/" + qr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(blockedIn.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var out = restTemplate.exchange(baseUrl() + "/vehicles/checkout/" + qr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.OK);

        var passBlocked = restTemplate.exchange(baseUrl() + "/me/vehicles/" + vehicleId + "/access-pass",
                HttpMethod.GET, new HttpEntity<>(authHeaders(residentToken)), Map.class);
        assertThat(passBlocked.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Reactivate: the old QR must stay dead, a new one is issued.
        var reactivated = restTemplate.exchange(baseUrl() + "/vehicles/" + vehicleId + "/approve",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(reactivated.getStatusCode()).isEqualTo(HttpStatus.OK);

        var oldQrAfterReactivate = restTemplate.exchange(baseUrl() + "/vehicles/checkin/" + qr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(oldQrAfterReactivate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var newPass = restTemplate.exchange(baseUrl() + "/me/vehicles/" + vehicleId + "/access-pass",
                HttpMethod.GET, new HttpEntity<>(authHeaders(residentToken)), Map.class);
        assertThat(newPass.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newPass.getBody().get("qrToken")).isNotEqualTo(qr);
    }
}
