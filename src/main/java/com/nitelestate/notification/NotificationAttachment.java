package com.nitelestate.notification;

/**
 * An EMAIL-only attachment (e.g. a generated payment receipt PDF — see
 * payment.PaymentService.sendReceipt). SMS/WhatsApp senders never see this; NotificationSender's
 * default send() implementation simply ignores it, same convention as subject/transactional.
 */
public record NotificationAttachment(String filename, String contentType, byte[] content) {
}
