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
 * Sends email through Twilio SendGrid's v3 Mail Send API. Same vendor family as the SMS/WhatsApp
 * sender (Twilio owns SendGrid) but a separate API key/product, so it's configured independently.
 *
 * Only registered when nitel.notifications.sendgrid.api-key is actually set (see
 * SendGridConfiguredCondition) - MockEmailSender takes over otherwise, same as TwilioSmsSender.
 */
@Slf4j
@Component
@Conditional(SendGridConfiguredCondition.class)
public class SendGridEmailSender implements NotificationSender {

    private static final String SUBJECT = "Nitel Estate Management System";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public SendGridEmailSender(
            @Value("${nitel.notifications.sendgrid.api-key:}") String apiKey,
            @Value("${nitel.notifications.sendgrid.from-email}") String fromEmail,
            @Value("${nitel.notifications.sendgrid.from-name}") String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.sendgrid.com/v3")
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
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", recipient)))),
                "from", Map.of("email", fromEmail, "name", fromName),
                "subject", SUBJECT,
                "content", List.of(Map.of("type", "text/plain", "value", message)));

        restClient.post()
                .uri("/mail/send")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
