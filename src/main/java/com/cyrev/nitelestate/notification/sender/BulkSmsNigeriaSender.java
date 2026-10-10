package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import com.cyrev.nitelestate.notification.Redaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends SMS through BulkSMSNigeria's v2 API (https://www.bulksmsnigeria.com/app/api/docs) —
 * replaces TwilioSmsSender as the SMS channel whenever it's configured (see
 * BulkSmsNigeriaConfiguredCondition / TwilioSmsSender's ConditionalOnMissingBean on this class).
 *
 * The provider routes transactional content (OTPs, verification codes) through a dedicated
 * "otp" gateway and rejects it on the regular bulk gateway (error BSNG-2013) - see
 * NotificationSender's 4-arg send. `transactional` picks that route; everything else (account
 * provisioning, announcements, receipt-shared alerts) uses the configured default gateway.
 */
@Slf4j
@Component
@Conditional(BulkSmsNigeriaConfiguredCondition.class)
public class BulkSmsNigeriaSender implements NotificationSender {

    private static final String OTP_GATEWAY = "otp";

    private final RestClient restClient;
    private final String apiToken;
    private final String senderId;
    private final String defaultGateway;

    public BulkSmsNigeriaSender(
            @Value("${nitel.notifications.bulksms.api-token:}") String apiToken,
            @Value("${nitel.notifications.bulksms.sender-id}") String senderId,
            @Value("${nitel.notifications.bulksms.gateway}") String defaultGateway,
            @Value("${nitel.notifications.bulksms.base-url}") String baseUrl) {
        this.apiToken = apiToken;
        this.senderId = senderId;
        this.defaultGateway = defaultGateway;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(String recipient, String message) {
        send(recipient, message, null, false);
    }

    @Override
    public void send(String recipient, String message, String subject, boolean transactional) {
        if (!StringUtils.hasText(apiToken)) {
            log.info("[SMS not configured, would send] to={} message={}", recipient, Redaction.redact(message));
            return;
        }
        Map<String, Object> body = Map.of(
                "from", senderId,
                "to", stripLeadingPlus(recipient),
                "body", message,
                "gateway", transactional ? OTP_GATEWAY : defaultGateway);

        restClient.post()
                .uri("/sms")
                .header("Authorization", "Bearer " + apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /** recipient is E.164 (e.g. "+2348023797036" - see PhoneNumbers) but the provider expects digits only. */
    private static String stripLeadingPlus(String recipient) {
        return recipient.startsWith("+") ? recipient.substring(1) : recipient;
    }
}
