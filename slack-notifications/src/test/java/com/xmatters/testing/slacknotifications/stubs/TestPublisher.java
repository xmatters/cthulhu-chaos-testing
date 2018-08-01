package com.xmatters.testing.slacknotifications.stubs;

import com.xmatters.testing.slacknotifications.NotificationPublisher;
import com.xmatters.testing.slacknotifications.SlackMessage;

import java.util.ArrayList;
import java.util.List;

public class TestPublisher implements NotificationPublisher {

    private List<SlackMessage> publications = new ArrayList<>();

    @Override
    public void publishMessage(SlackMessage message) throws Exception {
        publications.add(message);
    }

    public List<SlackMessage> getPublications() {
        return publications;
    }
}
