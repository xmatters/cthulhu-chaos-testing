package com.xmatters.testing.slacknotifications;

import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;

import java.util.HashMap;
import java.util.Map;

public class DebugRunner {

    private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
        put("slack.webhook.url", "https://hooks.slack.com/services/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");
        put("slack.username", "Cthulhu");
        put("slack.icon_emoji", ":cthulhu:");
        put("slack.audit.filter", "EVENT_RUNNING EVENT_UPDATE EVENT_WARNING EVENT_ERROR EVENT_ENDED CTHULHU_ENDED");
    }};

    public static void main(String[] args) throws Exception {
        SlackNotifier notifier = new SlackNotifier();
        notifier.setConfiguration(CONFIG);
        notifier.configure();

        for (ChaosAuditType chaosAuditType : ChaosAuditType.values()) {
            notifier.processEvent(chaosAuditType, String.join(" ", args));
        }
    }
}
