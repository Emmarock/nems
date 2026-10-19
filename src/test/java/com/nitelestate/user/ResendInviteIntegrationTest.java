package com.nitelestate.user;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the admin "resend invite" action: a resident login that's still pending (mustChangePassword
 * true, see UserService.resendInvite) can have a fresh "set your password" link sent again - e.g.
 * because the original one expired or the message was lost. Restricted to pending invites only -
 * an already-active account must refuse, since reissuing a reset-token link there would let
 * whoever receives the message take over a working login.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResendInviteIntegrationTest {

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
    void resendInvite_sendsFreshLink_forAPendingInviteOnly() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        Map<String, Object> newResident = Map.of(
                "fullName", "Pending Invite Resident",
                "phone", "+2348099990077",
                "email", "pending.invite@example.com",
                "residentType", "OWNER"
        );
        var created = restTemplate.postForEntity(baseUrl() + "/residents",
                new HttpEntity<>(newResident, authHeaders(adminToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long residentId = ((Number) created.getBody().get("id")).longValue();

        var bulkCreated = restTemplate.postForEntity(baseUrl() + "/users/bulk-create-residents",
                new HttpEntity<>(Map.of("residentIds", List.of(residentId)), authHeaders(adminToken)), Map.class);
        assertThat(bulkCreated.getStatusCode()).isEqualTo(HttpStatus.OK);

        var found = restTemplate.exchange(baseUrl() + "/users?q=Pending Invite Resident", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) found.getBody().get("content");
        assertThat(content).hasSize(1);
        Long userId = ((Number) content.get(0).get("id")).longValue();
        assertThat(content.get(0).get("mustChangePassword")).isEqualTo(true);
        assertThat(content.get(0).get("inviteLinkExpired")).isEqualTo(false);

        var resent = restTemplate.exchange(baseUrl() + "/users/" + userId + "/resend-invite", HttpMethod.POST,
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(resent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resent.getBody().get("mustChangePassword")).isEqualTo(true);
        assertThat(resent.getBody().get("inviteLinkExpired")).isEqualTo(false);
    }

    @Test
    void resendInvite_refusesForAnAlreadyActiveAccount() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        var adminUser = restTemplate.exchange(baseUrl() + "/users?q=Super Admin", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(adminUser.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) adminUser.getBody().get("content");
        Long activeUserId = ((Number) content.get(0).get("id")).longValue();
        assertThat(content.get(0).get("mustChangePassword")).isEqualTo(false);

        var refused = restTemplate.exchange(baseUrl() + "/users/" + activeUserId + "/resend-invite", HttpMethod.POST,
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
