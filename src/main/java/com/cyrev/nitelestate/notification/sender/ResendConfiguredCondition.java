package com.cyrev.nitelestate.notification.sender;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Same reasoning as TwilioSmsConfiguredCondition, checked against the Resend API key. */
class ResendConfiguredCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return StringUtils.hasText(context.getEnvironment().getProperty("RESEND_API_KEY"));
    }
}
