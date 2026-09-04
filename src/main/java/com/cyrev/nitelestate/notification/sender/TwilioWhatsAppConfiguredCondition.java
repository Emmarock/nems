package com.cyrev.nitelestate.notification.sender;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Same reasoning as TwilioSmsConfiguredCondition, checked against the WhatsApp sender number. */
class TwilioWhatsAppConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("TWILIO_ACCOUNT_SID"))
                && StringUtils.hasText(env.getProperty("TWILIO_AUTH_TOKEN"))
                && StringUtils.hasText(env.getProperty("TWILIO_WHATSAPP_FROM"));
    }
}
