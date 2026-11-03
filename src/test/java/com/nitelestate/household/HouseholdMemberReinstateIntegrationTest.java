package com.nitelestate.household;

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
 * A revoked household pass (e.g. switched off while the member travels) must be refused at the
 * gate, still allow check-out, and work again with the SAME QR once the landlord reinstates it.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HouseholdMemberReinstateIntegrationTest {

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
    void revokedPass_isRefusedAtGate_andWorksAgainAfterReinstate() {
        // The seeded resident owns their property, so they act as landlord here too.
        String landlordToken = login("resident@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        var created = restTemplate.postForEntity(baseUrl() + "/household-members",
                new HttpEntity<>(Map.of("fullName", "Travelling Spouse", "phone", "+2348011119001", "relationship", "SPOUSE"),
                        authHeaders(landlordToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long memberId = ((Number) created.getBody().get("id")).longValue();

        // No QR is issued until the landlord approves.
        String noQrYet = (String) created.getBody().get("qrToken");
        assertThat(noQrYet).isNull();

        var approved = restTemplate.exchange(baseUrl() + "/household-members/" + memberId + "/approve",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(landlordToken)), Map.class);
        String qr = (String) approved.getBody().get("qrToken");

        assertThat(checkIn(qr, securityToken)).isEqualTo(HttpStatus.OK);

        var revoked = restTemplate.exchange(baseUrl() + "/household-members/" + memberId + "/revoke",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(landlordToken)), Map.class);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(checkIn(qr, securityToken)).isEqualTo(HttpStatus.BAD_REQUEST);
        var out = restTemplate.exchange(baseUrl() + "/household-members/checkout/" + qr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(out.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Listed under the landlord's revoked filter.
        var revokedList = restTemplate.exchange(baseUrl() + "/household-members/property?status=REVOKED",
                HttpMethod.GET, new HttpEntity<>(authHeaders(landlordToken)), Map.class);
        assertThat(revokedList.getBody().toString()).contains("Travelling Spouse");

        var reinstated = restTemplate.exchange(baseUrl() + "/household-members/" + memberId + "/reinstate",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(landlordToken)), Map.class);
        assertThat(reinstated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reinstated.getBody().get("status")).isEqualTo("ACTIVE");
        assertThat(reinstated.getBody().get("qrToken")).isEqualTo(qr);

        assertThat(checkIn(qr, securityToken)).isEqualTo(HttpStatus.OK);

        // Reinstating an active pass is rejected.
        var again = restTemplate.exchange(baseUrl() + "/household-members/" + memberId + "/reinstate",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(landlordToken)), Map.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpStatus checkIn(String qr, String securityToken) {
        return (HttpStatus) restTemplate.exchange(baseUrl() + "/household-members/checkin/" + qr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class).getStatusCode();
    }
}
