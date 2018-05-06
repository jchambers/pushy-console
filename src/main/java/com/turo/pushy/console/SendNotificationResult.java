package com.turo.pushy.console;

import com.turo.pushy.apns.PushNotificationResponse;

public class SendNotificationResult {

    private final PushNotificationResponse response;

    public SendNotificationResult(final PushNotificationResponse response) {
        this.response = response;
    }

    public String getTopic() {
        return this.response.getPushNotification().getTopic();
    }

    public String getToken() {
        return this.response.getPushNotification().getToken();
    }

    public String getPayload() {
        return this.response.getPushNotification().getPayload();
    }

    public String getPriority() {
        // TODO Localize
        return this.response.getPushNotification().getPriority().name();
    }

    public String getStatus() {
        // TODO Localize
        return this.response.isAccepted() ? "Accepted" : "Rejected";
    }

    public String getDetails() {
        // TODO Localize
        // TODO Handle dates, too
        return this.response.isAccepted() ? "n/a" : response.getRejectionReason();
    }

    public String getApnsId() {
        return this.response.getApnsId().toString();
    }
}
