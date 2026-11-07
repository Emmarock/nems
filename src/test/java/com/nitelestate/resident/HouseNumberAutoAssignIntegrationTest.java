package com.nitelestate.resident;

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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Creating a resident with a new property but no house number gives the property the next
 * number on its road (HouseNumbers.next); a typed house number is still used as given.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HouseNumberAutoAssignIntegrationTest {

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

    private String create(String adminToken, String road, String houseNumber, String phone) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "fullName", "Auto Number " + phone,
                "phone", phone,
                "residentType", "LANDLORD",
                "road", road,
                "address", "Nitel Estate",
                "propertyType", "DETACHED_HOUSE"));
        if (houseNumber != null) {
            body.put("houseNumber", houseNumber);
        }
        var response = restTemplate.postForEntity(baseUrl() + "/residents/with-property",
                new HttpEntity<>(body, authHeaders(adminToken)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) ((Map<?, ?>) response.getBody().get("property")).get("houseNumber");
    }

    @Test
    void blankHouseNumber_getsNextNumberOnTheRoad() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        // First property on a new road starts at 001.
        assertThat(create(adminToken, "Zeta Close", null, "+2348070000001")).isEqualTo("ZETA CLOSE-001");
        // Typed numbers are kept as given, and later blanks continue after the highest one.
        assertThat(create(adminToken, "Zeta Close", "ZETA CLOSE-007", "+2348070000002")).isEqualTo("ZETA CLOSE-007");
        assertThat(create(adminToken, "zeta  close", "  ", "+2348070000003")).isEqualTo("ZETA CLOSE-008");
        // Other formats on the road (e.g. the imported NEW2 series) don't affect the count.
        assertThat(create(adminToken, "Zeta Close", "ZETA CLOSE-NEW2-050", "+2348070000004")).isEqualTo("ZETA CLOSE-NEW2-050");
        assertThat(create(adminToken, "Zeta Close", null, "+2348070000005")).isEqualTo("ZETA CLOSE-009");
    }
}
