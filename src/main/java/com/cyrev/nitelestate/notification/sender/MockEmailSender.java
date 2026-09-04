package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Local/dev default - takes over automatically whenever SendGridEmailSender isn't registered
 * (i.e. nitel.notifications.sendgrid.api-key isn't set). See SendGridConfiguredCondition. */
@Slf4j
@Component
@ConditionalOnMissingBean(SendGridEmailSender.class)
public class MockEmailSender implements NotificationSender {
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }
    @Override
    public void send(String recipient, String message) {
        log.info("[MOCK EMAIL] to={} message={}", recipient, message);
    }
}
