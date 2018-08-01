package com.xmatters.testing.cthulhu.stubs;

import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.annotations.OperationName;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

@EngineName(BrokenChaosEngine.BROKEN_ENGINE_NAME)
public class BrokenChaosEngine extends ChaosEngine {
    public static final String BROKEN_ENGINE_NAME = "broken-engine";

    @Override
    public void configure() throws Exception {
        throw new Exception("This Chaos Engine is intentionally broken.");
    }

    @Override
    public String[] getTargets(ChaosEvent ev) {
        return null;
    }

    @OperationName(MockChaosEngine.OP_DEFAULT)
    public void doTheThing(String[] targets) {
    }
}
