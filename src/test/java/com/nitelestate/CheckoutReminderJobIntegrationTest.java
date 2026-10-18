package com.nitelestate;

import com.nitelestate.notification.NotificationLogRepository;
import com.nitelestate.scheduling.CheckoutReminderJob;
import com.nitelestate.visitor.Visitor;
import com.nitelestate.visitor.VisitorRepository;
import com.nitelestate.visitor.VisitorStatus;
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
 * Proves the pre-departure countdown reminders actually dispatch:
 * CheckoutReminderJob.remindApproachingVisitorCheckouts (visitor due within 30 minutes) and
 * remindApproachingWorkerCurfew (non-domestic worker still on site). The cron schedule itself
 * isn't exercised here - that's Spring's job, not ours to re-test - just that calling the job
 * directly for a visitor due to leave soon / a worker still checked in sends the expected email.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CheckoutReminderJobIntegrationTest {

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
    private CheckoutReminderJob checkoutReminderJob;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private VisitorRepository visitorRepository;

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

    /** NotificationService.dispatch fires on a separate @Async thread (see its javadoc), so the
     * NotificationLog row may not be committed yet the instant the job method returns - poll
     * briefly rather than racing it. */
    private void waitForNotificationCountAbove(long baseline) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            if (notificationLogRepository.count() > baseline) {
                return;
            }
            Thread.sleep(50);
        }
    }

    @Test
    void approachingVisitorCheckout_withinThirtyMinutes_sendsReminder() throws InterruptedException {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");

        Map<String, Object> visitorRequest = Map.of(
                "name", "Due Soon Visitor",
                "phone", "+2348099990000",
                "validFrom", Instant.now().minus(1, ChronoUnit.HOURS).toString(),
                "validUntil", Instant.now().plus(15, ChronoUnit.MINUTES).toString()
        );
        var created = restTemplate.postForEntity(baseUrl() + "/visitors",
                new HttpEntity<>(visitorRequest, authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        long before = notificationLogRepository.count();
        checkoutReminderJob.remindApproachingVisitorCheckouts();
        waitForNotificationCountAbove(before);

        boolean found = notificationLogRepository.findAll().stream().anyMatch(n ->
                n.getChannel().name().equals("EMAIL") && n.getMessage().contains("Due Soon Visitor")
                        && n.getMessage().contains("ends in about"));
        assertThat(found).isTrue();
    }

    @Test
    void approachingWorkerCurfew_stillOnSite_sendsReminder() throws InterruptedException {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");
        String securityToken = login("security@nitelestate.local", "Passw0rd!");

        Map<String, Object> workerRequest = Map.of(
                "fullName", "Curfew Test Worker",
                "phone", "+2348099991111",
                "contractorName", "Dusk Services Ltd",
                "workType", "Cleaning",
                "workerCategory", "NON_DOMESTIC_STAFF",
                "startDate", LocalDate.now().toString(),
                "expectedEndDate", LocalDate.now().plusMonths(1).toString()
        );
        var created = restTemplate.postForEntity(baseUrl() + "/workers",
                new HttpEntity<>(workerRequest, authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String qrToken = (String) created.getBody().get("qrToken");

        var checkedIn = restTemplate.exchange(baseUrl() + "/workers/checkin/" + qrToken,
                HttpMethod.POST, new HttpEntity<>(authHeaders(securityToken)), Map.class);
        assertThat(checkedIn.getStatusCode()).isEqualTo(HttpStatus.OK);

        long before = notificationLogRepository.count();
        checkoutReminderJob.remindApproachingWorkerCurfew();
        waitForNotificationCountAbove(before);

        boolean found = notificationLogRepository.findAll().stream().anyMatch(n ->
                n.getChannel().name().equals("EMAIL") && n.getMessage().contains("Curfew Test Worker")
                        && n.getMessage().contains("needs to check out"));
        assertThat(found).isTrue();
    }

    /**
     * Proves the "already overdue" reminder's underlying selection query stops matching a pass
     * once it's been reminded about - the actual mechanism behind "fire once, then stop"
     * (remindLandlordsOfDueCheckouts is wall-clock-gated to 18:20-22:00 estate-local, so it can't
     * be exercised directly in a test that may run at any time of day; this checks the repository
     * contract CheckoutReminderJob.remindForVisitors relies on instead).
     */
    @Test
    void overdueVisitorCheckout_queryExcludesAlreadyReminded() {
        String residentToken = login("resident@nitelestate.local", "Passw0rd!");

        Map<String, Object> visitorRequest = Map.of(
                "name", "Already Overdue Visitor",
                "phone", "+2348099992222",
                "validFrom", Instant.now().minus(2, ChronoUnit.HOURS).toString(),
                "validUntil", Instant.now().minus(5, ChronoUnit.MINUTES).toString()
        );
        var created = restTemplate.postForEntity(baseUrl() + "/visitors",
                new HttpEntity<>(visitorRequest, authHeaders(residentToken)), Map.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long visitorId = ((Number) created.getBody().get("id")).longValue();

        Instant endOfToday = LocalDate.now(java.time.ZoneId.of("Africa/Lagos")).plusDays(1)
                .atStartOfDay(java.time.ZoneId.of("Africa/Lagos")).toInstant();

        assertThat(visitorRepository.findByStatusAndValidUntilLessThanAndCheckoutReminderSentFalse(
                        VisitorStatus.ACTIVE, endOfToday))
                .extracting(Visitor::getId)
                .contains(visitorId);

        Visitor visitor = visitorRepository.findById(visitorId).orElseThrow();
        visitor.setCheckoutReminderSent(true);
        visitorRepository.save(visitor);

        assertThat(visitorRepository.findByStatusAndValidUntilLessThanAndCheckoutReminderSentFalse(
                        VisitorStatus.ACTIVE, endOfToday))
                .extracting(Visitor::getId)
                .doesNotContain(visitorId);
    }
}
