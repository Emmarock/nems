package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Sends WhatsApp messages through Twilio's Messages API (same endpoint as SMS, "whatsapp:"
 * prefix on To/From). Twilio's WhatsApp Sandbox lets this work end-to-end for testing before
 * going through Meta Business verification for a production sender number - see the console's
 * Messaging > Try it out > WhatsApp page for the sandbox number and join code.
 *
 * Outside a 24-hour customer-initiated session, WhatsApp requires a pre-approved message
 * template rather than free text - a proactive send (e.g. a new-account welcome, an
 * announcement) may be rejected by Twilio/Meta once a real production sender is live, even
 * though this call succeeds unconditionally against the sandbox. That's a template-registration
 * step on Twilio's side, not something to fix here.
 *
 * Only registered when nitel.notifications.twilio is actually configured (see
 * TwilioWhatsAppConfiguredCondition) - MockWhatsAppSender takes over otherwise, same as
 * TwilioSmsSender.
 */
@Slf4j
@Component
@Conditional(TwilioWhatsAppConfiguredCondition.class)
public class TwilioWhatsAppSender implements NotificationSender {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioWhatsAppSender(
            @Value("${nitel.notifications.twilio.account-sid:}") String accountSid,
            @Value("${nitel.notifications.twilio.auth-token:}") String authToken,
            @Value("${nitel.notifications.twilio.whatsapp-from:}") String from) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.from = from;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01/Accounts/" + accountSid)
                .requestInterceptor(new BasicAuthenticationInterceptor(accountSid, authToken))
                .build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }

    @Override
    public void send(String recipient, String message) {
        if (!isConfigured()) {
            log.info("[WhatsApp not configured, would send] to={} message={}", recipient, message);
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", "whatsapp:" + recipient);
        form.add("From", "whatsapp:" + from);
        form.add("Body", message);

        restClient.post()
                .uri("/Messages.json")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    private boolean isConfigured() {
        return StringUtils.hasText(accountSid) && StringUtils.hasText(authToken) && StringUtils.hasText(from);
    }
}
