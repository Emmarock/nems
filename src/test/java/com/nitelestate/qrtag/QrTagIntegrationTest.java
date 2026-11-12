package com.nitelestate.qrtag;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pre-printed QR tags: generate a batch as a PDF, link a tag to a vehicle by its printed code (or
 * scanned URL), and the tag becomes the vehicle's gate pass. A tag is never linked twice, kinds
 * can't be mixed, and only the admins link property tags.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QrTagIntegrationTest {

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

    private Number registerVehicle(String residentToken, String plate) {
        var created = restTemplate.postForEntity(baseUrl() + "/me/vehicles",
                new HttpEntity<>(Map.of("plateNumber", plate), authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (Number) created.getBody().get("id");
    }

    private String passToken(String adminToken, Number vehicleId) {
        var pass = restTemplate.exchange(baseUrl() + "/vehicles/" + vehicleId + "/access-pass",
                HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(pass.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) pass.getBody().get("qrToken");
    }

    private org.springframework.http.ResponseEntity<Map> assign(String token, String kind, Number subjectId, String code) {
        return restTemplate.exchange(baseUrl() + "/qr-tags/assign", HttpMethod.POST,
                new HttpEntity<>(Map.of("kind", kind, "subjectId", subjectId, "code", code), authHeaders(token)), Map.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void printedTag_linksToVehicle_andBecomesItsGatePass() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        var batch = restTemplate.exchange(baseUrl() + "/qr-tags/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("kind", "VEHICLE", "count", 3), authHeaders(adminToken)), byte[].class);
        assertThat(batch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(batch.getBody(), 0, 5)).isEqualTo("%PDF-");
        String batchNo = batch.getHeaders().getFirst("X-Batch-No");

        var batches = restTemplate.exchange(baseUrl() + "/qr-tags/batches", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), List.class);
        Map<String, Object> summary = ((List<Map<String, Object>>) batches.getBody()).stream()
                .filter(b -> String.valueOf(b.get("batchNo")).equals(batchNo)).findFirst().orElseThrow();
        assertThat(((Number) summary.get("total")).intValue()).isEqualTo(3);
        assertThat(((Number) summary.get("linked")).intValue()).isZero();
        String firstCode = (String) summary.get("firstCode");   // e.g. V-000001
        int firstSerial = Integer.parseInt(firstCode.substring(2));

        Number vehicleId = registerVehicle(residentToken, "TAG-001");
        String oldPass = passToken(adminToken, vehicleId);

        // Typed loosely ("v1" for V-000001) by security, who hands out vehicle stickers.
        var linked = assign(securityToken, "VEHICLE", vehicleId, "v" + firstSerial);
        assertThat(linked.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(linked.getBody().get("code")).isEqualTo(firstCode);
        assertThat(linked.getBody().get("status")).isEqualTo("ASSIGNED");

        // The tag's token is now the vehicle's pass; the old in-app pass is dead.
        String tagToken = passToken(adminToken, vehicleId);
        assertThat(tagToken).isNotEqualTo(oldPass);
        var lookup = restTemplate.exchange(baseUrl() + "/vehicles/lookup/" + tagToken,
                HttpMethod.GET, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(lookup.getBody().get("plateNumber")).isEqualTo("TAG-001");
        assertThat(restTemplate.exchange(baseUrl() + "/vehicles/lookup/" + oldPass,
                HttpMethod.GET, new HttpEntity<>(authHeaders(securityToken)), Map.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var current = restTemplate.exchange(baseUrl() + "/qr-tags/current?kind=VEHICLE&subjectId=" + vehicleId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(current.getBody().get("code")).isEqualTo(firstCode);

        // A tag is only ever linked once.
        Number otherVehicle = registerVehicle(residentToken, "TAG-002");
        assertThat(assign(adminToken, "VEHICLE", otherVehicle, firstCode).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Re-tagging the first vehicle (scanned URL this time) voids its old sticker for good.
        String secondCode = "V-" + String.format("%06d", firstSerial + 1);
        var relinked = assign(adminToken, "VEHICLE", vehicleId, "https://estate.example/scan/vehicle/"
                + tokenForCode(adminToken, otherVehicle, secondCode));
        assertThat(relinked.getStatusCode()).isEqualTo(HttpStatus.CONFLICT); // that tag went to otherVehicle just now

        // Kinds can't be mixed, and only the admins link property tags.
        var propertyBatch = restTemplate.exchange(baseUrl() + "/qr-tags/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("kind", "PROPERTY", "count", 1), authHeaders(adminToken)), byte[].class);
        assertThat(propertyBatch.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assign(adminToken, "VEHICLE", otherVehicle, "P-000001").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(assign(securityToken, "PROPERTY", 1L, "P-000001").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Residents can't generate tags.
        assertThat(restTemplate.exchange(baseUrl() + "/qr-tags/batches", HttpMethod.POST,
                new HttpEntity<>(Map.of("kind", "VEHICLE", "count", 1), authHeaders(residentToken)), String.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void treasurerAndFinancialSecretary_canPrintAndLinkTags() {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String treasurerToken = login("treasurer@nitelestate.local", "Passw0rd!");
        var created = restTemplate.postForEntity(baseUrl() + "/users", new HttpEntity<>(Map.of(
                "email", "finsec.tags@nitelestate.local", "password", "Tmp-Pass-2026!xyz",
                "fullName", "Fin Sec", "role", "FINANCIAL_SECRETARY"), authHeaders(adminToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // Admin-created accounts must set their own password before they get their role's rights.
        String firstToken = login("finsec.tags@nitelestate.local", "Tmp-Pass-2026!xyz");
        assertThat(restTemplate.exchange(baseUrl() + "/auth/password", HttpMethod.PUT, new HttpEntity<>(Map.of(
                "currentPassword", "Tmp-Pass-2026!xyz", "newPassword", "Own-Pass-2026!xyz"), authHeaders(firstToken)), String.class)
                .getStatusCode().is2xxSuccessful()).isTrue();
        String finSecToken = login("finsec.tags@nitelestate.local", "Own-Pass-2026!xyz");

        var property = restTemplate.postForEntity(baseUrl() + "/properties", new HttpEntity<>(Map.of(
                "road", "Tag Road", "houseNumber", "TAG-1", "address", "Nitel Estate", "propertyType", "DETACHED_HOUSE"),
                authHeaders(adminToken)), Map.class);
        assertThat(property.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number propertyId = (Number) property.getBody().get("id");

        for (String token : new String[]{treasurerToken, finSecToken}) {
            var batch = restTemplate.exchange(baseUrl() + "/qr-tags/batches", HttpMethod.POST,
                    new HttpEntity<>(Map.of("kind", "PROPERTY", "count", 1), authHeaders(token)), byte[].class);
            assertThat(batch.getStatusCode()).isEqualTo(HttpStatus.OK);
            String batchNo = batch.getHeaders().getFirst("X-Batch-No");
            assertThat(restTemplate.exchange(baseUrl() + "/qr-tags/batches/" + batchNo + "/pdf", HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)), byte[].class).getStatusCode()).isEqualTo(HttpStatus.OK);

            List<?> batches = restTemplate.exchange(baseUrl() + "/qr-tags/batches", HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)), List.class).getBody();
            String code = (String) ((Map<?, ?>) batches.get(0)).get("firstCode");
            var linked = assign(token, "PROPERTY", propertyId, code);
            assertThat(linked.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(linked.getBody().get("status")).isEqualTo("ASSIGNED");
        }
    }

    /** Links {@code code} to {@code vehicleId} and returns the tag's token (read back from the vehicle's pass). */
    private String tokenForCode(String adminToken, Number vehicleId, String code) {
        assertThat(assign(adminToken, "VEHICLE", vehicleId, code).getStatusCode()).isEqualTo(HttpStatus.OK);
        return passToken(adminToken, vehicleId);
    }
}
