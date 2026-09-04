package com.cyrev.nitelestate.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationService(NotificationLogRepository notificationLogRepository,
                                ApplicationEventPublisher eventPublisher,
                                List<NotificationSender> senders) {
        this.notificationLogRepository = notificationLogRepository;
        this.eventPublisher = eventPublisher;
        this.senders = senders.stream().collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    /**
     * Queues a notification rather than sending it inline - callers (UserService,
     * AnnouncementService) never block on an outbound Twilio/SendGrid call, and a slow/failing
     * provider can't stretch out an unrelated HTTP request. See handle() for the actual send.
     */
    public void dispatch(NotificationChannel channel, String recipient, String message) {
        eventPublisher.publishEvent(new NotificationRequestedEvent(channel, recipient, message));
    }

    /**
     * Runs off-thread, after the publishing transaction commits (fallbackExecution=true covers
     * the rare case dispatch() is ever called outside a transaction, so the event still fires
     * instead of being silently dropped - the default @TransactionalEventListener behaviour).
     * Committing first means nothing gets sent for a User/Announcement that ends up rolled back,
     * and the original request's DB connection isn't held open for the duration of the send.
     *
     * PORTAL requires no dispatch (residents see it when they load the portal) — logged as SENT.
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(NotificationRequestedEvent event) {
        NotificationLog entry = new NotificationLog();
        entry.setRecipient(event.recipient());
        entry.setChannel(event.channel());
        entry.setMessage(event.message());

        NotificationSender sender = senders.get(event.channel());
        try {
            if (sender != null) {
                sender.send(event.recipient(), event.message());
            }
            entry.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            log.warn("Notification dispatch failed channel={} recipient={}", event.channel(), event.recipient(), ex);
            entry.setStatus(NotificationStatus.FAILED);
        }
        notificationLogRepository.save(entry);
    }
}
