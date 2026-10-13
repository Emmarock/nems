package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import com.cyrev.nitelestate.notification.Redaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends SMS through BulkSMSNigeria's v2 API (https://www.bulksmsnigeria.com/app/api/docs) —
 * replaces TwilioSmsSender as the SMS channel whenever it's configured (see
 * BulkSmsNigeriaConfiguredCondition / TwilioSmsSender's ConditionalOnMissingBean on this class),
 * but backs off in turn the moment TermiiSmsSender is configured - see TermiiSmsSender's own
 * javadoc for why that one now takes priority.
 *
 * The provider routes transactional content (OTPs, verification codes) through a dedicated
 * "otp" gateway and rejects it on the regular bulk gateway - see NotificationSender's 4-arg
 * send. `transactional` picks that route; everything else (account provisioning, announcements,
 * receipt-shared alerts) uses the configured default gateway.
 */
@Slf4j
@Component
@Conditional(BulkSmsNigeriaConfiguredCondition.class)
@ConditionalOnMissingBean(TermiiSmsSender.class)
public class BulkSmsNigeriaSender implements NotificationSender {

    private static final String OTP_GATEWAY = "otp";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
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

        ResponseEntity<String> response = restClient.post()
                .uri("/sms")
                .header("Authorization", "Bearer " + apiToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        // The provider can return HTTP 200 even for a submission it doesn't actually accept -
        // the real result is in the body: {"status":"success"|"failed", "code":"BSNG-xxxx",
        // "message":"...", "data":{...}}. toBodilessEntity() used to discard this entirely, so a
        // rejected submission looked identical to an accepted one here and in the Notification
        // Log: logged SENT, nothing ever reaching a phone. Always log the raw response too, so a
        // shape this check doesn't recognize is still visible in the service logs.
        //
        // NOTE: a "success" status here only means the provider's SMSC accepted and billed the
        // submission (see data.cost/data.message_id) - it is NOT a delivery confirmation. Actual
        // delivery depends on the recipient's network and, in Nigeria, on the sender ID being
        // DND-bypass approved on that network; a "success" response with the message never
        // arriving almost always means the sender ID isn't approved yet or the recipient number
        // is DND-registered, not a bug in this call. BulkSMSNigeria's own dashboard shows the
        // per-message delivery report (DLR) for data.message_id, which this synchronous response
        // never carries.
        String responseBody = response.getBody();
        log.info("BulkSMSNigeria response to={} status={} body={}", recipient, response.getStatusCode(), responseBody);
        String providerError = extractError(responseBody);
        if (providerError != null) {
            throw new IllegalStateException("BulkSMSNigeria rejected the message: " + providerError);
        }
    }

    /** Null when the response reports success (or doesn't parse) - a parse hiccup alone shouldn't
     * flip a send that otherwise looks fine into a false FAILED; the raw body is logged either way. */
    private String extractError(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            String status = json.path("status").asText("");
            if ("success".equalsIgnoreCase(status)) {
                return null;
            }
            String code = json.path("code").asText("");
            String message = json.path("message").asText("");
            if (!StringUtils.hasText(status) && !StringUtils.hasText(code) && !StringUtils.hasText(message)) {
                return null;
            }
            return (StringUtils.hasText(code) ? code + ": " : "") + (StringUtils.hasText(message) ? message : status);
        } catch (Exception e) {
            return null;
        }
    }

    /** recipient is E.164 (e.g. "+2348023797036" - see PhoneNumbers) but the provider expects digits only. */
    private static String stripLeadingPlus(String recipient) {
        return recipient.startsWith("+") ? recipient.substring(1) : recipient;
    }
}
