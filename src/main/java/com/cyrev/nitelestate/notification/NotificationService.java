package com.cyrev.nitelestate.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationService(NotificationLogRepository notificationLogRepository, List<NotificationSender> senders) {
        this.notificationLogRepository = notificationLogRepository;
        this.senders = senders.stream().collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    /** PORTAL requires no dispatch (residents see it when they load the portal) — logged as SENT. */
    @Transactional
    public void dispatch(NotificationChannel channel, String recipient, String message) {
        NotificationLog entry = new NotificationLog();
        entry.setRecipient(recipient);
        entry.setChannel(channel);
        entry.setMessage(message);

        NotificationSender sender = senders.get(channel);
        try {
            if (sender != null) {
                sender.send(recipient, message);
            }
            entry.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            log.warn("Notification dispatch failed channel={} recipient={}", channel, recipient, ex);
            entry.setStatus(NotificationStatus.FAILED);
        }
        notificationLogRepository.save(entry);
    }
}
