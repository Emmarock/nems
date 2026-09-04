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
 * Sends SMS through Twilio's Messages API. recipient must already be a dispatchable E.164 number
 * (see PhoneNumbers) - Twilio rejects anything else outright.
 *
 * Only registered when nitel.notifications.twilio is actually configured (see
 * TwilioSmsConfiguredCondition) - MockSmsSender takes over otherwise, so local/dev and an
 * unconfigured prod keep working with zero setup.
 */
@Slf4j
@Component
@Conditional(TwilioSmsConfiguredCondition.class)
public class TwilioSmsSender implements NotificationSender {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioSmsSender(
            @Value("${nitel.notifications.twilio.account-sid:}") String accountSid,
            @Value("${nitel.notifications.twilio.auth-token:}") String authToken,
            @Value("${nitel.notifications.twilio.sms-from:}") String from) {
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
        return NotificationChannel.SMS;
    }

    @Override
    public void send(String recipient, String message) {
        if (!isConfigured()) {
            log.info("[SMS not configured, would send] to={} message={}", recipient, message);
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", recipient);
        form.add("From", from);
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
