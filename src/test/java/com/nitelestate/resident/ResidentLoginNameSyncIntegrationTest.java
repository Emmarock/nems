package com.nitelestate.resident;

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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the fix for a real reported bug: app_user.fullName is a one-time copy of
 * Resident.fullName (taken at account-creation/claim time) that nothing used to push a later
 * rename into - so the header/login name (sourced from the login response) silently diverged
 * from the resident's actual current name shown elsewhere (the dashboard/profile, which reads
 * Resident.fullName live). ResidentService.update/updateSelf now call syncLoginFullName to keep
 * the linked login row's name in step with every edit, same pattern as the existing
 * syncLoginPhone. V35__app_user_resident_fk_and_name_sync.sql backfills any already-diverged rows
 * and adds the FK app_user.resident_id never had.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResidentLoginNameSyncIntegrationTest {

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
    void adminRenamingAResident_updatesTheLinkedLoginsDisplayName() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        // Starting name deliberately not asserted here - the other test in this class may have
        // already renamed this same seeded resident first (JUnit doesn't guarantee method
        // order); all this test cares about is that ITS OWN rename propagates to the login.
        var current = restTemplate.exchange(baseUrl() + "/residents/1", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(current.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> renameRequest = Map.of(
                "fullName", "Mr Emmanuel Udoh",
                "phone", current.getBody().get("phone"),
                "email", current.getBody().get("email"),
                "propertyId", current.getBody().get("propertyId"),
                "residentType", current.getBody().get("residentType"),
                "emergencyContact", ""
        );
        var renamed = restTemplate.exchange(baseUrl() + "/residents/1", HttpMethod.PUT,
                new HttpEntity<>(renameRequest, authHeaders(adminToken)), Map.class);
        assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);

        // The login response's fullName is read from app_user.fullName (see CustomUserDetails,
        // AuthController.login) - this is exactly the "header" name the bug report was about.
        var loginAfterRename = restTemplate.postForEntity(baseUrl() + "/auth/login",
                Map.of("identifier", "resident@nitelestate.local", "password", "Passw0rd!"), Map.class);
        assertThat(loginAfterRename.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginAfterRename.getBody().get("fullName")).isEqualTo("Mr Emmanuel Udoh");
    }

    @Test
    void adminChangingAResidentsPhone_updatesTheLinkedLoginsPhone() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        var current = restTemplate.exchange(baseUrl() + "/residents/1", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(current.getStatusCode()).isEqualTo(HttpStatus.OK);

        String newPhone = "+2348011119999";
        Map<String, Object> rephoneRequest = Map.of(
                "fullName", current.getBody().get("fullName"),
                "phone", newPhone,
                "email", current.getBody().get("email"),
                "propertyId", current.getBody().get("propertyId"),
                "residentType", current.getBody().get("residentType"),
                "emergencyContact", ""
        );
        var rephoned = restTemplate.exchange(baseUrl() + "/residents/1", HttpMethod.PUT,
                new HttpEntity<>(rephoneRequest, authHeaders(adminToken)), Map.class);
        assertThat(rephoned.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Previously admin updates never called syncLoginPhone at all (only updateSelf did) - the
        // login's phone would have stayed on whatever it was at account-creation time. Logging in
        // with the NEW phone number proves app_user.phone actually moved with it this time.
        String tokenViaNewPhone = login(newPhone, "Passw0rd!");
        assertThat(tokenViaNewPhone).isNotBlank();
    }

    @Test
    void residentRenamingThemselves_updatesTheirOwnLoginsDisplayName() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");

        var me = restTemplate.exchange(baseUrl() + "/me", HttpMethod.GET,
                new HttpEntity<>(authHeaders(residentToken)), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> residentBody = (Map<String, Object>) me.getBody().get("resident");

        Map<String, Object> selfRenameRequest = Map.of(
                "fullName", "Self Renamed Resident",
                "phone", residentBody.get("phone"),
                "email", residentBody.get("email") == null ? "" : residentBody.get("email"),
                "emergencyContact", ""
        );
        var renamed = restTemplate.exchange(baseUrl() + "/me/profile", HttpMethod.PUT,
                new HttpEntity<>(selfRenameRequest, authHeaders(residentToken)), Map.class);
        assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);

        var loginAfterRename = restTemplate.postForEntity(baseUrl() + "/auth/login",
                Map.of("identifier", "resident@nitelestate.local", "password", "Passw0rd!"), Map.class);
        assertThat(loginAfterRename.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginAfterRename.getBody().get("fullName")).isEqualTo("Self Renamed Resident");
    }
}
