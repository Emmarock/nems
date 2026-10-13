package com.nitelestate.notification.sender;

import com.nitelestate.notification.NotificationChannel;
import com.nitelestate.notification.NotificationSender;
import com.nitelestate.notification.Redaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends SMS through Termii's Messaging API (https://developers.termii.com/messaging-api) —
 * takes over the SMS channel from both BulkSmsNigeriaSender and TwilioSmsSender whenever it's
 * configured (see TermiiConfiguredCondition / those two senders' ConditionalOnMissingBean on
 * this class), so switching providers is just setting TERMII_API_KEY - no need to unset the
 * others first.
 *
 * Every message this app sends is effectively transactional (account provisioning, OTPs,
 * payment confirmations, checkout/arrears reminders) - never promotional - so this always uses
 * Termii's "dnd" channel, not "generic". Termii's own docs are explicit that "generic" is
 * DND-restricted and time-windowed (no delivery 8pm-8am WAT for MTN) and warn that using it for
 * OTPs/transactional content "may result in eventual delivery failures or Sender ID being
 * blocked" - "dnd" bypasses DND registration entirely and has no time restriction, at the cost
 * of a slightly higher per-message price. The `transactional` hint (BulkSmsNigeriaSender's
 * OTP-vs-bulk-gateway split) doesn't apply here since there's only one channel to pick.
 */
@Slf4j
@Component
@Conditional(TermiiConfiguredCondition.class)
public class TermiiSmsSender implements NotificationSender {

    private static final String CHANNEL = "dnd";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RestClient restClient;
    private final String apiKey;
    private final String senderId;

    public TermiiSmsSender(
            @Value("${nitel.notifications.termii.api-key:}") String apiKey,
            @Value("${nitel.notifications.termii.sender-id}") String senderId,
            @Value("${nitel.notifications.termii.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.senderId = senderId;
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
        if (!StringUtils.hasText(apiKey)) {
            log.info("[SMS not configured, would send] to={} message={}", recipient, Redaction.redact(message));
            return;
        }
        Map<String, Object> body = Map.of(
                "to", stripLeadingPlus(recipient),
                "from", senderId,
                "sms", message,
                "type", "plain",
                "channel", CHANNEL,
                "api_key", apiKey);

        ResponseEntity<String> response = restClient.post()
                .uri("/api/sms/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        // Documented success shape: {"code":"ok","balance":...,"message_id":...,
        // "message":"Successfully Sent","user":...}. Undocumented failure shape (Termii's own
        // API docs show only the success case) - same defensive pattern as
        // BulkSmsNigeriaSender's fix: always log the raw response so anything this check
        // doesn't recognize is still visible, and only throw on a "code" we can positively
        // identify as not "ok" (a hard auth/validation error already throws via the HTTP status
        // above, through RestClient's own default behaviour).
        String responseBody = response.getBody();
        log.info("Termii response to={} status={} body={}", recipient, response.getStatusCode(), responseBody);
        String providerError = extractError(responseBody);
        if (providerError != null) {
            throw new IllegalStateException("Termii rejected the message: " + providerError);
        }
    }

    /** Null when the response reports "ok" (or doesn't parse) - a parse hiccup alone shouldn't
     * flip a send that otherwise looks fine into a false FAILED; the raw body is logged either way. */
    private String extractError(String responseBody) {
        if (responseBody == null) {
            return null;
        }
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            JsonNode codeNode = json.get("code");
            if (codeNode == null || codeNode.isNull()) {
                return null;
            }
            String code = codeNode.asText("");
            if ("ok".equalsIgnoreCase(code)) {
                return null;
            }
            String message = json.path("message").asText("");
            return StringUtils.hasText(message) ? code + ": " + message : code;
        } catch (Exception e) {
            return null;
        }
    }

    /** recipient is E.164 (e.g. "+2348023797036" - see PhoneNumbers) but the provider expects digits only. */
    private static String stripLeadingPlus(String recipient) {
        return recipient.startsWith("+") ? recipient.substring(1) : recipient;
    }
}
