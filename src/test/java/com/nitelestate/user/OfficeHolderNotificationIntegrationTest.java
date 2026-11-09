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

import com.nitelestate.notification.NotificationLog;
import com.nitelestate.notification.NotificationLogRepository;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Office notifications go to the personal email on the office account when set, otherwise to the
 * resident the super admin assigned to that office (OfficeHolder).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OfficeHolderNotificationIntegrationTest {

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

    private boolean emailed(String to, String text) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (notificationLogRepository.findAll().stream()
                    .anyMatch(l -> l.getRecipient().equalsIgnoreCase(to) && l.getMessage().contains(text))) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private Number submitReceipt(String residentToken, Number levyId) {
        var receipt = restTemplate.postForEntity(baseUrl() + "/me/payments/receipts", new HttpEntity<>(Map.of(
                "levyId", levyId, "amount", 2500, "method", "BANK_TRANSFER"), authHeaders(residentToken)), Map.class);
        assertThat(receipt.getStatusCode().is2xxSuccessful()).isTrue();
        return (Number) receipt.getBody().get("id");
    }

    @Test
    void officeEmail_goesToAssignedResident_unlessOfficeHasPersonalEmail() throws InterruptedException {
        String adminToken = login("admin@nitelestate.local", "Passw0rd!");
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");

        // A resident who holds the Treasurer office.
        var holder = restTemplate.postForEntity(baseUrl() + "/residents/with-property", new HttpEntity<>(Map.of(
                "fullName", "Mrs Treasurer", "phone", "+2348077700001", "email", "treasurer.holder@example.com",
                "residentType", "OWNER", "road", "Office Road", "address", "Nitel Estate",
                "propertyType", "DETACHED_HOUSE"), authHeaders(adminToken)), Map.class);
        assertThat(holder.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number holderId = (Number) ((Map<?, ?>) holder.getBody().get("resident")).get("id");

        var assigned = restTemplate.exchange(baseUrl() + "/office-holders/TREASURER", HttpMethod.PUT,
                new HttpEntity<>(Map.of("residentId", holderId), authHeaders(adminToken)), Map.class);
        assertThat(assigned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(assigned.getBody().get("residentEmail")).isEqualTo("treasurer.holder@example.com");

        // Only the super admin manages office holders.
        String treasurerToken = login("treasurer@nitelestate.local", "Passw0rd!");
        assertThat(restTemplate.exchange(baseUrl() + "/office-holders", HttpMethod.GET,
                new HttpEntity<>(authHeaders(treasurerToken)), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        var levy = restTemplate.postForEntity(baseUrl() + "/levies", new HttpEntity<>(Map.of(
                "name", "Office Holder Levy", "amount", 2500, "frequency", "ONE_TIME", "active", true), authHeaders(adminToken)), Map.class);
        Number levyId = (Number) levy.getBody().get("id");

        // No personal email on the treasurer account: the assigned resident is emailed.
        Number first = submitReceipt(residentToken, levyId);
        assertThat(emailed("treasurer.holder@example.com", "/payments?review=" + first)).isTrue();

        // Once the treasurer account sets a personal email, that takes precedence.
        restTemplate.exchange(baseUrl() + "/account/office-profile", HttpMethod.PUT,
                new HttpEntity<>(Map.of("personalEmail", "treasurer.personal@example.com"), authHeaders(treasurerToken)), Map.class);
        Number second = submitReceipt(residentToken, levyId);
        assertThat(emailed("treasurer.personal@example.com", "/payments?review=" + second)).isTrue();
        assertThat(notificationLogRepository.findAll().stream().anyMatch(l ->
                l.getRecipient().equalsIgnoreCase("treasurer.holder@example.com")
                        && l.getMessage().contains("/payments?review=" + second))).isFalse();
    }
}
