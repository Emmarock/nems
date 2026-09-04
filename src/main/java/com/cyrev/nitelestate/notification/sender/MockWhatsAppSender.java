package com.cyrev.nitelestate.notification.sender;

import com.cyrev.nitelestate.notification.NotificationChannel;
import com.cyrev.nitelestate.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** Local/dev default - takes over automatically whenever TwilioWhatsAppSender isn't registered
 * (i.e. nitel.notifications.twilio isn't fully configured). See TwilioWhatsAppConfiguredCondition. */
@Slf4j
@Component
@ConditionalOnMissingBean(TwilioWhatsAppSender.class)
public class MockWhatsAppSender implements NotificationSender {
    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WHATSAPP;
    }
    @Override
    public void send(String recipient, String message) {
        log.info("[MOCK WHATSAPP] to={} message={}", recipient, message);
    }
}
