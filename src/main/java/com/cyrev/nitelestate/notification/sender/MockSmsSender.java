package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import com.cyrev.nitelestate.notification.Redaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Local/dev default - takes over automatically whenever neither TwilioSmsSender nor
 * BulkSmsNigeriaSender is registered (i.e. no real SMS provider is configured). */
@Slf4j
@Component
@ConditionalOnMissingBean({TwilioSmsSender.class, BulkSmsNigeriaSender.class})
public class MockSmsSender implements NotificationSender {
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
        log.info("[MOCK SMS] to={} transactional={} message={}", recipient, transactional, Redaction.redact(message));
    }
}
