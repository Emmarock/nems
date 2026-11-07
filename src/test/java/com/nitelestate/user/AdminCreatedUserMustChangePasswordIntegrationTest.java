package com.nitelestate.user;

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
 * An account created by an admin starts with a temporary password: logging in with it works but
 * reports mustChangePassword, so the frontend sends the user to set their own.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCreatedUserMustChangePasswordIntegrationTest {

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
    void adminCreatedUser_mustChangePasswordOnFirstLogin() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        Map<String, Object> user = Map.of(
                "email", "new.officer@nitelestate.local",
                "password", "Tmp-Pass-2026!xyz",
                "fullName", "New Officer",
                "role", "SECURITY");
        var created = restTemplate.postForEntity(baseUrl() + "/users",
                new HttpEntity<>(user, authHeaders(adminToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var firstLogin = restTemplate.postForEntity(baseUrl() + "/auth/login",
                Map.of("identifier", "new.officer@nitelestate.local", "password", "Tmp-Pass-2026!xyz"), Map.class);
        assertThat(firstLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstLogin.getBody().get("mustChangePassword")).isEqualTo(true);
    }
}
