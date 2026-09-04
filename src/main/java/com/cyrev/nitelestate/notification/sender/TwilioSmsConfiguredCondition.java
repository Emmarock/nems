package com.cyrev.nitelestate.notification.sender;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * True only when real Twilio SMS credentials are actually set. Checked against the raw env var
 * names rather than the YAML-bound nitel.notifications.twilio.* properties, because a
 * "${TWILIO_ACCOUNT_SID:}" blank default would otherwise still count as a "present" property
 * under Spring's usual property-conditional checks - this needs "blank counts as absent"
 * semantics, since TwilioSmsSender must lose to MockSmsSender whenever nothing real is configured.
 */
class TwilioSmsConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("TWILIO_ACCOUNT_SID"))
                && StringUtils.hasText(env.getProperty("TWILIO_AUTH_TOKEN"))
                && StringUtils.hasText(env.getProperty("TWILIO_SMS_FROM"));
    }
}
