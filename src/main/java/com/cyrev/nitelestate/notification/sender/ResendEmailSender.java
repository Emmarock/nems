package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends email through Resend's Emails API — used for every outbound email, including the
 * "your account is ready" message a resident/tenant gets the moment UserService provisions
 * their login (see UserService.notifyNewAccount). The plain-text message every caller already
 * builds is wrapped in EmailTemplate's branded HTML layout and sent alongside a plain-text
 * fallback, so every email this sends looks the same regardless of which caller dispatched it.
 *
 * Only registered when nitel.notifications.resend.api-key is actually set (see
 * ResendConfiguredCondition) - MockEmailSender takes over otherwise, same as TwilioSmsSender.
 */
@Slf4j
@Component
@Conditional(ResendConfiguredCondition.class)
public class ResendEmailSender implements NotificationSender {

    private static final String SUBJECT = "Nitel Estate Management System";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public ResendEmailSender(
            @Value("${nitel.notifications.resend.api-key:}") String apiKey,
            @Value("${nitel.notifications.resend.from-email}") String fromEmail,
            @Value("${nitel.notifications.resend.from-name}") String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(String recipient, String message) {
        if (!StringUtils.hasText(apiKey)) {
            log.info("[Email not configured, would send] to={} message={}", recipient, message);
            return;
        }
        Map<String, Object> body = Map.of(
                "from", fromName + " <" + fromEmail + ">",
                "to", List.of(recipient),
                "subject", SUBJECT,
                "html", EmailTemplate.render(message),
                "text", message);

        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
