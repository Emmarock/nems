package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import com.cyrev.nitelestate.notification.Redaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Local/dev default - takes over automatically whenever ResendEmailSender isn't registered
 * (i.e. nitel.notifications.resend.api-key isn't set). See ResendConfiguredCondition. */
@Slf4j
@Component
@ConditionalOnMissingBean(ResendEmailSender.class)
public class MockEmailSender implements NotificationSender {
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }
    @Override
    public void send(String recipient, String message) {
        send(recipient, message, null, false);
    }

    @Override
    public void send(String recipient, String message, String subject, boolean transactional) {
        log.info("[MOCK EMAIL] to={} subject={} message={}", recipient, subject, Redaction.redact(message));
    }
}
