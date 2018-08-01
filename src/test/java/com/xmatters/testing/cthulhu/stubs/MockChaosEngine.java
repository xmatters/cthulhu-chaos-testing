package com.xmatters.testing.cthulhu.stubs;

import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.annotations.OperationName;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

import java.util.Collection;
import java.util.stream.Collectors;

@EngineName(MockChaosEngine.ENGINE_NAME)
public class MockChaosEngine extends ChaosEngine {
    public static final String ENGINE_NAME = "mock-engine";
    public static final String OP_DEFAULT = "do the thing";
    public static final String OP_LOG_CONFIG = "log configuration";
    public static final String OP_LONG_EVENT = "Long event";

    @Override
    public void configure() throws Exception {
        this.sendUpdate(UpdateType.INFO, "initialization");
    }

    @Override
    public MockCustomTarget[] getTargets(ChaosEvent ev) {
        Collection<MockCustomTarget> targets = ev.getTargetParts("a", "b", "c", "d").values().stream()
                .map(s -> new MockCustomTarget(s))
                .collect(Collectors.toList());

        MockCustomTarget[] stockArr = new MockCustomTarget[targets.size()];
        return targets.toArray(stockArr);
    }

    @OperationName(MockChaosEngine.OP_DEFAULT)
    public void doTheThing(MockCustomTarget[] targets) {
        for (MockCustomTarget t : targets) {
            this.sendUpdate(UpdateType.INFO, t.toString());
        }
    }

    @OperationName(OP_LONG_EVENT)
    public void longEvent(MockCustomTarget[] targets) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            this.sendUpdate(UpdateType.INFO, e.getMessage());
        }
    }

    @OperationName(OP_LOG_CONFIG)
    public void logConfiguration(MockCustomTarget[] targets) {
        String[] keys = new String[]{"config.a", "config.b"};
        for (String key : keys) {
            this.sendUpdate(UpdateType.INFO, key, this.getConfigValue(key));
        }
    }

    public class MockCustomTarget {
        private String targetValue;

        MockCustomTarget(String s) {
            targetValue = s;
        }

        @Override
        public String toString() {
            return targetValue;
        }
    }
}

