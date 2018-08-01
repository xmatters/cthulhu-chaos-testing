package com.xmatters.testing.slacknotifications;

import com.google.common.base.Strings;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditor;
import com.xmatters.testing.cthulhu.api.configuration.ModuleConfiguration;

import javax.naming.ConfigurationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SlackNotifier extends ModuleConfiguration implements ChaosAuditor {

    public static final String CONFIG_AUDIT_FILTER = "slack.audit.filter";
    public static final String CONFIG_AUDIT_TYPE_MESSAGE = "slack.audit.%s.message";
    public static final String CONFIG_AUDIT_TYPE_MESSAGE_PLACEHOLDER = "%s";
    public static final String CONFIG_CHANNELS = "slack.channels";
    public static final String CONFIG_ELEMENT_SEPARATOR = " ";
    public static final String SLACK_ICON_EMOJI = "slack.icon.emoji";
    public static final String CONFIG_USERNAME = "slack.username";
    public static final String CONFIG_WEBHOOK_URL = "slack.webhook.url";

    private NotificationPublisher publisher = null;

    private Set<ChaosAuditType> auditFilter = null;
    private List<String> channelList = new ArrayList<>();
    private String iconEmoji;
    private String username;

    private Map<ChaosAuditType, String> messageFormat = new HashMap<>();

    @Override
    public void configure() throws Exception {
        String webhookURL = this.getConfigValue(CONFIG_WEBHOOK_URL);
        if (Strings.isNullOrEmpty(webhookURL)) {
            throw new ConfigurationException(CONFIG_WEBHOOK_URL + " is required.");
        }
        configure(new HttpPublisher(new URI(webhookURL)));
    }

    public void configure(NotificationPublisher publisher) {
        this.publisher = publisher;

        username = this.getConfigValue(CONFIG_USERNAME);
        iconEmoji = this.getConfigValue(SLACK_ICON_EMOJI);

        String channels = this.getConfigValue(CONFIG_CHANNELS);
        if (!Strings.isNullOrEmpty(channels)) {
            channelList = Arrays.stream(channels.split(CONFIG_ELEMENT_SEPARATOR))
                    .filter(c -> !Strings.isNullOrEmpty(c))
                    .collect(Collectors.toList());
        }

        String auditTypeFilters = this.getConfigValue(CONFIG_AUDIT_FILTER);
        if (!Strings.isNullOrEmpty(auditTypeFilters)) {
            auditFilter = Arrays.stream(auditTypeFilters.split(CONFIG_ELEMENT_SEPARATOR))
                    .filter(auditType -> !Strings.isNullOrEmpty(auditType))
                    .map(filter -> ChaosAuditType.valueOf(filter))
                    .collect(Collectors.toSet());
        }

        Arrays.stream(ChaosAuditType.values()).forEach(type -> {
            String message = this.getConfigValue(String.format(CONFIG_AUDIT_TYPE_MESSAGE, type.name()), type.name() + ": %s");
            messageFormat.put(type, message);
        });
    }

    @Override
    public void processEvent(ChaosAuditType auditType, String... messages) throws Exception {
        if (auditFilter != null && !auditFilter.contains(auditType)) {
            return;
        }

        String content = messageFormat.get(auditType)
                .replace(CONFIG_AUDIT_TYPE_MESSAGE_PLACEHOLDER, String.join(CONFIG_ELEMENT_SEPARATOR, messages));

        if (channelList.size() == 0) {
            publisher.publishMessage(makeMessage(content));
        } else {
            for (String channel : channelList) {
                publisher.publishMessage(makeMessage(content, channel));
            }
        }
    }

    private SlackMessage makeMessage(String content) {
        return makeMessage(content, null);
    }

    private SlackMessage makeMessage(String content, String channel) {
        SlackMessage message = new SlackMessage();

        message.setChannel(channel);
        message.setIconEmoji(iconEmoji);
        message.setText(content);
        message.setUsername(username);

        return message;
    }
}
