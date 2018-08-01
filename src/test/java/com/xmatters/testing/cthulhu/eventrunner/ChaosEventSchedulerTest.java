package com.xmatters.testing.cthulhu.eventrunner;

import com.google.common.collect.Sets;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.api.scenario.Duration;
import com.xmatters.testing.cthulhu.api.scenario.Schedule;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import com.xmatters.testing.cthulhu.stubs.MockChaosEngine;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ChaosEventSchedulerTest {

    private static final ChaosEvent FIVE_REPETITIONS = new ChaosEvent() {{
        setDescription("A test event");
        setEngine(MockChaosEngine.ENGINE_NAME);
        setOperation(MockChaosEngine.OP_DEFAULT);
        setSchedule(new Schedule() {{
            setDelay(new Duration("1s"));
            setRepeat(5);
        }});
        setTarget("namespace/instance");
    }};

    private static final ChaosEvent TIMEOUT_EVENT = new ChaosEvent() {{
        setDescription("An event that times out");
        setEngine(MockChaosEngine.ENGINE_NAME);
        setOperation(MockChaosEngine.OP_LONG_EVENT);
        setTarget("namespace/instance");
        setTimeout(new Duration("500ms"));
    }};

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
    @SuppressWarnings("unchecked")
    public void scheduleAnEvent() {
        ChaosEventRunner testRunner = new ChaosEventRunner();
        testRunner.loadOperationsFromChaosEngines(Sets.newHashSet(MockChaosEngine.class));

        List<Thread> threadPool = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        ChaosEventScheduler scheduler = new ChaosEventScheduler(testRunner, threadPool, makeAuditingService(errors, ChaosAuditType.EVENT_ERROR));

        StopWatch timer = new StopWatch() {{
            start();
        }};
        scheduler.schedule(FIVE_REPETITIONS);
        scheduler.waitForEnd();
        timer.stop();

        assertEquals(5, threadPool.size());
        assertTrue("The threads were expected to complete in about 5000ms.  It took " + Long.toString(timer.getTotalTimeMillis()) + "ms",
                5000 <= timer.getTotalTimeMillis() && timer.getTotalTimeMillis() <= 5100);
        assertEquals("Expected completion without errors.", 0, errors.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void eventTimeout() {
        ChaosEventRunner testRunner = new ChaosEventRunner();
        testRunner.loadOperationsFromChaosEngines(Sets.newHashSet(MockChaosEngine.class));

        List<Thread> threadPool = new ArrayList<>();
        List<String> updates = new ArrayList<>();
        ChaosEventScheduler scheduler = new ChaosEventScheduler(testRunner, threadPool, makeAuditingService(updates));

        StopWatch timer = new StopWatch() {{
            start();
        }};
        scheduler.schedule(TIMEOUT_EVENT);
        scheduler.waitForEnd();
        timer.stop();

        assertTrue("The threads were expected to get cancelled in about 500ms.  It took " + Long.toString(timer.getTotalTimeMillis()) + "ms",
                500 <= timer.getTotalTimeMillis() && timer.getTotalTimeMillis() <= 600);
    }
}