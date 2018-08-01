package com.xmatters.testing.slacknotifications;

public interface NotificationPublisher {
    void publishMessage(SlackMessage message) throws Exception;
}
