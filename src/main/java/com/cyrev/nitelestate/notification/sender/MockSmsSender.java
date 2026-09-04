package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Local/dev default - takes over automatically whenever TwilioSmsSender isn't registered
 * (i.e. nitel.notifications.twilio isn't fully configured). See TwilioSmsConfiguredCondition. */
@Slf4j
@Component
@ConditionalOnMissingBean(TwilioSmsSender.class)
public class MockSmsSender implements NotificationSender {
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }
    @Override
    public void send(String recipient, String message) {
        log.info("[MOCK SMS] to={} message={}", recipient, message);
    }
}
