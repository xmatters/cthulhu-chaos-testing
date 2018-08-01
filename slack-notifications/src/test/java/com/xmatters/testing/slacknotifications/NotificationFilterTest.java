package com.xmatters.testing.slacknotifications;

import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.slacknotifications.stubs.TestPublisher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class NotificationFilterTest {
    @Parameters
    public static Collection<Object[]> data() {
        Collection<Object[]> data = Arrays.stream(ChaosAuditType.values())
                .map(auditType -> new Object[]{new ChaosAuditType[]{auditType}})
                .collect(Collectors.toSet());

        data.add(new Object[]{new ChaosAuditType[]{ChaosAuditType.CTHULHU_STARTED, ChaosAuditType.EVENT_RUNNING, ChaosAuditType.EVENT_UPDATE}});

        return data;
    }

    @Parameter
    public ChaosAuditType[] filterSetup;

    @Test
    public void filterAudits() throws Exception {
        List<String> expectedPublicationTypes = Arrays.stream(filterSetup)
                .map(Enum::toString)
                .collect(Collectors.toList());

        TestPublisher pub = new TestPublisher();
        SlackNotifier notifier = new SlackNotifier() {{
            setConfiguration(new HashMap<String, String>() {{
                put(SlackNotifier.CONFIG_AUDIT_FILTER, String.join(" ", expectedPublicationTypes));
            }});
            configure(pub);
        }};

        for (ChaosAuditType auditType : ChaosAuditType.values()) {
            notifier.processEvent(auditType, "Test message");
        }

        Assert.assertEquals(expectedPublicationTypes.size(), pub.getPublications().size());
        for (int i = 0; i < expectedPublicationTypes.size(); i++) {
            String expectedType = expectedPublicationTypes.get(i);
            Assert.assertEquals(expectedType + ": Test message", pub.getPublications().get(i).getText());
        }
    }

    @Test
    public void customMessages() throws Exception {
        List<String> expectedPublicationTypes = Arrays.stream(filterSetup)
                .map(Enum::toString)
                .collect(Collectors.toList());

        TestPublisher pub = new TestPublisher();
        SlackNotifier notifier = new SlackNotifier() {{
            setConfiguration(new HashMap<String, String>() {{
                put(SlackNotifier.CONFIG_AUDIT_FILTER, String.join(" ", expectedPublicationTypes));
                expectedPublicationTypes.forEach(type -> {
                    String cfg = String.format(SlackNotifier.CONFIG_AUDIT_TYPE_MESSAGE, type);
                    put(cfg, "This is a custom message (%s)");
                });
            }});
            configure(pub);
        }};

        for (ChaosAuditType auditType : ChaosAuditType.values()) {
            notifier.processEvent(auditType, "Test message");
        }

        Assert.assertEquals(expectedPublicationTypes.size(), pub.getPublications().size());
        for (int i = 0; i < expectedPublicationTypes.size(); i++) {
            Assert.assertEquals("This is a custom message (Test message)", pub.getPublications().get(i).getText());
        }
    }
}