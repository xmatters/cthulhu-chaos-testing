package com.xmatters.testing.cthulhu.eventrunner;

import com.google.common.collect.Sets;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import com.xmatters.testing.cthulhu.exceptions.ChaosEventHandlerException;
import com.xmatters.testing.cthulhu.stubs.BrokenChaosEngine;
import com.xmatters.testing.cthulhu.stubs.MockChaosEngine;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChaosEventRunnerTest {

    private static final int BASE_AUDIT_COUNT = 3;

    private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
        put("config.a", "value a");
        put("config.b", "value b");
    }};

    private ChaosEvent getDefaultEvent(String targets) {
        ChaosEvent ev = new ChaosEvent();
        ev.setDescription("A test event");
        ev.setEngine(MockChaosEngine.ENGINE_NAME);
        ev.setOperation(MockChaosEngine.OP_DEFAULT);
        ev.setTarget(targets);

        return ev;
    }

    @SuppressWarnings("unchecked")
    private ChaosEventRunner getRunner(List<String> auditRecords) {
        ChaosEventRunner runner = new ChaosEventRunner(makeAuditingService(auditRecords), CONFIG);
        runner.loadOperationsFromChaosEngines(Sets.newHashSet(MockChaosEngine.class, BrokenChaosEngine.class));

        return runner;
    }

    private static AuditingService makeAuditingService(List<String> updates, ChaosAuditType... filter) {
        return new AuditingService() {{
            registerChaosAuditor((auditType, messages) -> {
                if(filter == null || filter.length == 0 || Arrays.stream(filter).anyMatch(f -> f == auditType)) {
                    updates.add(auditType.toString() + ": " + String.join(" ", Arrays.asList(messages)));
                }
            });
        }};
    }

    @Test
    public void configurationIsPassedToEngine() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        ChaosEvent ev = getDefaultEvent("cluster/namespace");
        ev.setOperation(MockChaosEngine.OP_LOG_CONFIG);

        runner.execute(ev);
        Assert.assertEquals(BASE_AUDIT_COUNT + 2, recordedAudits.size());
        Assert.assertEquals("EVENT_UPDATE: config.a value a", recordedAudits.get(2));
        Assert.assertEquals("EVENT_UPDATE: config.b value b", recordedAudits.get(3));
    }

    @Test
    public void runAnEvent() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        runner.execute(getDefaultEvent("cluster/namespace"));
        Assert.assertEquals(BASE_AUDIT_COUNT + 2, recordedAudits.size());
        Assert.assertEquals("EVENT_RUNNING: A test event", recordedAudits.get(1));
        assertMatch("EVENT_UPDATE: (cluster|namespace)", recordedAudits.get(2));
        assertMatch("EVENT_UPDATE: (cluster|namespace)", recordedAudits.get(3));
        Assert.assertNotEquals(recordedAudits.get(2), recordedAudits.get(3));
        Assert.assertEquals("EVENT_ENDED: A test event", recordedAudits.get(4));
    }

    @Test
    public void eventRunnersAreInitialized() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        runner.execute(getDefaultEvent("cluster/namespace"));
        Assert.assertEquals(BASE_AUDIT_COUNT + 2, recordedAudits.size());
        Assert.assertEquals("EVENT_UPDATE: initialization", recordedAudits.get(0));
    }

    @Test
    public void restrictTargets() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        ChaosEvent ev = getDefaultEvent("cluster/namespace/instance/interface");
        ev.setQuantity(2);
        runner.execute(ev);
        Assert.assertEquals(BASE_AUDIT_COUNT + 2, recordedAudits.size());
    }

    @Test
    public void skipTargets() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        ChaosEvent ev = getDefaultEvent("cluster/namespace/instance/interface");
        ev.setSkip(1);
        runner.execute(ev);
        Assert.assertEquals(BASE_AUDIT_COUNT + 3, recordedAudits.size());
    }

    @Test
    public void skipBeforeTaking() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        ChaosEvent ev = getDefaultEvent("cluster/namespace/instance/interface");
        ev.setSkip(2);
        ev.setQuantity(3);
        runner.execute(ev);
        Assert.assertEquals(BASE_AUDIT_COUNT + 2, recordedAudits.size());
    }

    @Test
    public void doNotRegisterHandlersWhenInitFails() {
        List<String> recordedAudits = new ArrayList<>();
        ChaosEventRunner runner = getRunner(recordedAudits);

        ChaosEvent ev = getDefaultEvent("cluster/namespace");
        ev.setEngine(BrokenChaosEngine.BROKEN_ENGINE_NAME);

        assertThrow(ChaosEventHandlerException.class, "No candidates.*", () -> runner.execute(ev));
    }

    private void assertThrow(Class<? extends Throwable> clazz, String messagePattern, Runnable call) {
        Throwable caughtException = null;
        try {
            call.run();
        } catch (Throwable e) {
            caughtException = e;
        }

        Assert.assertNotNull("Expected " + clazz.getSimpleName() + " to be thrown.", caughtException);
        Assert.assertEquals(clazz, caughtException.getClass());
        assertMatch(messagePattern, caughtException.getMessage());
    }

    private void assertMatch(String expectedPattern, String actualString) {
        String failureMessage = String.format("Expected %s to match the pattern %s.", actualString, expectedPattern);
        Assert.assertTrue(failureMessage, actualString.matches(expectedPattern));
    }
}