package com.nitelestate.payment;

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

import java.util.List;
import java.util.Map;

import com.nitelestate.notification.NotificationLog;
import com.nitelestate.notification.NotificationLogRepository;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Receipt review: a resident's receipt emails the review offices (at a personal email when set)
 * with a link to the payment; the secretary can approve it; the first decision is final for
 * everyone except the super admin, who alone can revert it to awaiting approval.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentReceiptReviewIntegrationTest {

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

    @Autowired
    private NotificationLogRepository notificationLogRepository;

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

    private Map<?, ?> put(String path, Object body, String token) {
        var r = restTemplate.exchange(baseUrl() + path, HttpMethod.PUT, new HttpEntity<>(body, authHeaders(token)), Map.class);
        assertThat(r.getStatusCode()).as("PUT " + path).isEqualTo(HttpStatus.OK);
        return r.getBody();
    }

    /** The resident's own balance: [totalPaid, outstanding]. */
    private java.math.BigDecimal[] balance(String residentToken) {
        var r = restTemplate.exchange(baseUrl() + "/me/account/balance", HttpMethod.GET,
                new HttpEntity<>(authHeaders(residentToken)), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        return new java.math.BigDecimal[] {
                new java.math.BigDecimal(r.getBody().get("totalPaid").toString()),
                new java.math.BigDecimal(r.getBody().get("outstanding").toString())};
    }

    private HttpStatus putStatus(String path, String token) {
        return HttpStatus.valueOf(restTemplate.exchange(baseUrl() + path, HttpMethod.PUT,
                new HttpEntity<>(Map.of(), authHeaders(token)), Map.class).getStatusCode().value());
    }

    @Test
    void receiptIsEmailedToOffices_secretaryDecides_onlySuperAdminReverts() throws InterruptedException {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");

        // A secretary account; admin-created accounts must set their own password first.
        var created = restTemplate.postForEntity(baseUrl() + "/users", new HttpEntity<>(Map.of(
                "email", "secretary.office@nitelestate.local", "password", "Temp-Pass-123!",
                "fullName", "Estate Secretary", "role", "SECRETARY"), authHeaders(adminToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String tempToken = login("secretary.office@nitelestate.local", "Temp-Pass-123!");
        restTemplate.exchange(baseUrl() + "/auth/password", HttpMethod.PUT, new HttpEntity<>(Map.of(
                "currentPassword", "Temp-Pass-123!", "newPassword", "Secretary-Pass-456!"), authHeaders(tempToken)), Void.class);
        String secretaryToken = login("secretary.office@nitelestate.local", "Secretary-Pass-456!");

        // The secretary sets a personal email for office notifications.
        Map<?, ?> profile = put("/account/office-profile", Map.of("personalEmail", "secretary.personal@example.com"), secretaryToken);
        assertThat(profile.get("personalEmail")).isEqualTo("secretary.personal@example.com");

        // Residents have no office profile.
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        assertThat(restTemplate.exchange(baseUrl() + "/account/office-profile", HttpMethod.GET,
                new HttpEntity<>(authHeaders(residentToken)), Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // A levy, and a resident's receipt against it.
        var levy = restTemplate.postForEntity(baseUrl() + "/levies", new HttpEntity<>(Map.of(
                "name", "Receipt Review Levy", "amount", 5000, "frequency", "ONE_TIME", "active", true), authHeaders(adminToken)), Map.class);
        assertThat(levy.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number levyId = (Number) levy.getBody().get("id");
        var receipt = restTemplate.postForEntity(baseUrl() + "/me/payments/receipts", new HttpEntity<>(Map.of(
                "levyId", levyId, "amount", 5000, "method", "BANK_TRANSFER"), authHeaders(residentToken)), Map.class);
        assertThat(receipt.getStatusCode().is2xxSuccessful()).isTrue();
        Number paymentId = (Number) receipt.getBody().get("id");

        // The secretary is emailed at their personal address, with a link to the payment.
        String link = "/payments?review=" + paymentId;
        NotificationLog email = null;
        for (int i = 0; i < 50 && email == null; i++) {
            email = notificationLogRepository.findAll().stream()
                    .filter(l -> l.getRecipient().equals("secretary.personal@example.com") && l.getMessage().contains(link))
                    .findFirst().orElse(null);
            if (email == null) Thread.sleep(200);
        }
        assertThat(email).as("office email to the secretary's personal address").isNotNull();

        // The secretary approves; the decision is then final for every reviewer.
        java.math.BigDecimal[] beforeApproval = balance(residentToken);
        assertThat(put("/payments/" + paymentId + "/approve", Map.of("notes", "Matches bank alert"), secretaryToken)
                .get("status")).isEqualTo("SUCCESS");
        java.math.BigDecimal[] afterApproval = balance(residentToken);
        assertThat(afterApproval[0].subtract(beforeApproval[0])).isEqualByComparingTo("5000");
        assertThat(beforeApproval[1].subtract(afterApproval[1])).isEqualByComparingTo("5000");
        String treasurerToken = login("treasurer@nitelestate.local", "Passw0rd!");
        assertThat(putStatus("/payments/" + paymentId + "/reject", treasurerToken)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(putStatus("/payments/" + paymentId + "/approve", secretaryToken)).isEqualTo(HttpStatus.BAD_REQUEST);

        // Only the super admin can revert it.
        assertThat(putStatus("/payments/" + paymentId + "/revert", treasurerToken)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(putStatus("/payments/" + paymentId + "/revert", secretaryToken)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(put("/payments/" + paymentId + "/revert", Map.of("notes", "Wrong levy"), adminToken)
                .get("status")).isEqualTo("PENDING_APPROVAL");
        // Reverting an approval puts the amount back on the outstanding balance...
        java.math.BigDecimal[] afterRevert = balance(residentToken);
        assertThat(afterRevert[0]).isEqualByComparingTo(beforeApproval[0]);
        assertThat(afterRevert[1]).isEqualByComparingTo(beforeApproval[1]);
        // ...and the resident is told, with the reason.
        boolean residentTold = false;
        for (int i = 0; i < 50 && !residentTold; i++) {
            residentTold = notificationLogRepository.findAll().stream().anyMatch(l ->
                    l.getRecipient().equals("resident@nitelestate.local")
                            && l.getMessage().contains("has been reversed") && l.getMessage().contains("Wrong levy"));
            if (!residentTold) Thread.sleep(200);
        }
        assertThat(residentTold).as("resident emailed about the reversed approval").isTrue();
        long reversalEmails = notificationLogRepository.findAll().stream()
                .filter(l -> l.getMessage().contains("has been reversed")).count();

        // Back in review, it can be decided again. A rejection never touches the balance...
        assertThat(put("/payments/" + paymentId + "/reject", Map.of("notes", "No matching alert"), treasurerToken)
                .get("status")).isEqualTo("REJECTED");
        assertThat(balance(residentToken)[1]).isEqualByComparingTo(beforeApproval[1]);
        // ...and neither does reverting it - nor does it email the resident (nothing was confirmed to them).
        assertThat(put("/payments/" + paymentId + "/revert", Map.of(), adminToken).get("status")).isEqualTo("PENDING_APPROVAL");
        assertThat(balance(residentToken)[1]).isEqualByComparingTo(beforeApproval[1]);
        Thread.sleep(1000);
        assertThat(notificationLogRepository.findAll().stream()
                .filter(l -> l.getMessage().contains("has been reversed")).count()).isEqualTo(reversalEmails);
    }

    @Test
    void pendingDigest_listsEveryPaymentAwaitingApproval_superAdminOnly() throws InterruptedException {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String treasurerToken = login("treasurer@nitelestate.local", "Passw0rd!");
        put("/account/office-profile", Map.of("personalEmail", "treasurer.digest@example.com"), treasurerToken);

        var levy = restTemplate.postForEntity(baseUrl() + "/levies", new HttpEntity<>(Map.of(
                "name", "Digest Levy", "amount", 1000, "frequency", "ONE_TIME", "active", true), authHeaders(adminToken)), Map.class);
        Number levyId = (Number) levy.getBody().get("id");
        var receipt = restTemplate.postForEntity(baseUrl() + "/me/payments/receipts", new HttpEntity<>(Map.of(
                "levyId", levyId, "amount", 1000, "method", "BANK_TRANSFER"), authHeaders(residentToken)), Map.class);
        Number paymentId = (Number) receipt.getBody().get("id");

        assertThat(restTemplate.postForEntity(baseUrl() + "/payments/pending-digest",
                new HttpEntity<>(authHeaders(treasurerToken)), Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var digest = restTemplate.postForEntity(baseUrl() + "/payments/pending-digest",
                new HttpEntity<>(authHeaders(adminToken)), Map.class);
        assertThat(digest.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) digest.getBody().get("pending")).intValue()).isGreaterThanOrEqualTo(1);

        boolean found = false;
        for (int i = 0; i < 50 && !found; i++) {
            found = notificationLogRepository.findAll().stream().anyMatch(l ->
                    l.getRecipient().equals("treasurer.digest@example.com")
                            && l.getMessage().contains("awaiting approval")
                            && l.getMessage().contains("/payments?review=" + paymentId)
                            && l.getMessage().contains("/payments?status=PENDING_APPROVAL"));
            if (!found) Thread.sleep(200);
        }
        assertThat(found).as("digest emailed to the treasurer's personal email").isTrue();
    }
}
