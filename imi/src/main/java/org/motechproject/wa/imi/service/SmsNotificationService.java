package org.motechproject.wa.imi.service;

/**
 * Initiate sms notification with IMI
 */
public interface SmsNotificationService {

    /**
     * Used to initiate sms workflow with IMI
     * @param callingNumber phone number to send sms to
     * @param content sms content to send
     */
    String sendSms(Long callingNumber, String content, Integer courseId);
}
