package com.nitelestate.accesspolicy;

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
 * End-to-end proof that a sponsoring resident's arrears block ENTRY (not exit) for their guests,
 * household members, and workers — the behavior this was built for: previously this was only an
 * advisory flag, and only for the resident's own pass/vehicle on check-IN; now it's a hard
 * refusal for VISITOR/WORKER/HOUSEHOLD_MEMBER check-in specifically, with check-out always
 * exempt (see AccessPolicyService.requireSponsorEligibleForEntry).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessPolicyEntryBlockIntegrationTest {

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
    void sponsorArrears_blocksEntryButNotExit_forVisitorWorkerAndHouseholdMember() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        // --- Visitor A checks in BEFORE the resident owes anything - must succeed. ---
        Map<String, Object> visitorARequest = Map.of(
                "name", "Visitor Before Arrears",
                "phone", "+2348011110001",
                "validFrom", Instant.now().minus(1, ChronoUnit.HOURS).toString(),
                "validUntil", Instant.now().plus(1, ChronoUnit.DAYS).toString()
        );
        var visitorACreated = restTemplate.postForEntity(baseUrl() + "/visitors",
                new HttpEntity<>(visitorARequest, authHeaders(residentToken)), Map.class);
        assertThat(visitorACreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long hostResidentId = ((Number) visitorACreated.getBody().get("hostResidentId")).longValue();
        String visitorAEntryQr = (String) visitorACreated.getBody().get("qrTokenEntry");

        var visitorACheckedIn = restTemplate.postForEntity(
                baseUrl() + "/visitors/scan/" + visitorAEntryQr, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(visitorACheckedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> visitorABody = (Map<String, Object>) visitorACheckedIn.getBody().get("visitor");
        String visitorAExitQr = (String) visitorABody.get("qrTokenExit");
        assertThat(visitorAExitQr).isNotBlank();

        // --- Put the resident in arrears: generate an invoice, never pay it. "Security Levy"
        // (id 2 from the dev seed) rather than "Estate Levy" (id 1) - V10 deletes id 1 as unused,
        // even in a fresh test database seeded straight from the dev-seed migration.
        var invoiceGenerated = restTemplate.postForEntity(baseUrl() + "/invoices",
                new HttpEntity<>(Map.of("residentId", hostResidentId, "levyId", 2), authHeaders(adminToken)), Map.class);
        assertThat(invoiceGenerated.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // --- Visitor A, already inside, must still be able to leave despite the arrears. ---
        var visitorACheckedOut = restTemplate.postForEntity(
                baseUrl() + "/visitors/scan/" + visitorAExitQr, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(visitorACheckedOut.getStatusCode()).isEqualTo(HttpStatus.OK);

        // --- A NEW visitor for the same (now in-arrears) resident must be refused entry. ---
        Map<String, Object> visitorBRequest = Map.of(
                "name", "Visitor After Arrears",
                "phone", "+2348011110002",
                "validFrom", Instant.now().minus(1, ChronoUnit.HOURS).toString(),
                "validUntil", Instant.now().plus(1, ChronoUnit.DAYS).toString()
        );
        var visitorBCreated = restTemplate.postForEntity(baseUrl() + "/visitors",
                new HttpEntity<>(visitorBRequest, authHeaders(residentToken)), Map.class);
        assertThat(visitorBCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String visitorBEntryQr = (String) visitorBCreated.getBody().get("qrTokenEntry");

        var visitorBBlocked = restTemplate.postForEntity(
                baseUrl() + "/visitors/scan/" + visitorBEntryQr, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(visitorBBlocked.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // --- A worker sponsored by the same resident must also be refused entry. ---
        Map<String, Object> workerRequest = Map.of(
                "fullName", "Worker Sponsored By Debtor",
                "phone", "+2348011110003",
                "contractorName", "Test Contractors Ltd",
                "workType", "Plumbing",
                "workerCategory", "NON_DOMESTIC_STAFF",
                "startDate", LocalDate.now().toString(),
                "expectedEndDate", LocalDate.now().plusMonths(1).toString()
        );
        var workerCreated = restTemplate.postForEntity(baseUrl() + "/workers",
                new HttpEntity<>(workerRequest, authHeaders(residentToken)), Map.class);
        assertThat(workerCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String workerQr = (String) workerCreated.getBody().get("qrToken");

        var workerBlocked = restTemplate.exchange(baseUrl() + "/workers/checkin/" + workerQr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(workerBlocked.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // --- A household member sponsored by the same resident must also be refused entry. ---
        Map<String, Object> householdMemberRequest = Map.of(
                "fullName", "Tenant Of Debtor",
                "phone", "+2348011110004",
                "relationship", "SPOUSE"
        );
        var memberCreated = restTemplate.postForEntity(baseUrl() + "/household-members",
                new HttpEntity<>(householdMemberRequest, authHeaders(residentToken)), Map.class);
        assertThat(memberCreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long memberId = ((Number) memberCreated.getBody().get("id")).longValue();

        var memberApproved = restTemplate.exchange(baseUrl() + "/household-members/" + memberId + "/approve",
                HttpMethod.PUT, new HttpEntity<>(authHeaders(residentToken)), Map.class);
        assertThat(memberApproved.getStatusCode()).isEqualTo(HttpStatus.OK);
        String memberQr = (String) memberApproved.getBody().get("qrToken");

        var memberBlocked = restTemplate.exchange(baseUrl() + "/household-members/checkin/" + memberQr,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(memberBlocked.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
