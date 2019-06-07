package com.xmatters.testing.kubernetes;

import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.HashMap;
import java.util.Map;

public class DebugRunner {

    private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
        put("application.name", "cthulhu");
        put("run.name", "debug-run");

        // put("kube.config", "/path/to/.kube/config");
    }};

    public static void main(String[] args) throws Exception {
        KubeChaosEngine runner = new KubeChaosEngine();
        runner.setConfiguration(CONFIG);
        runner.setUpdateCollector(DebugRunner::updateCollector);
        runner.configure();

        ChaosEvent ev = new ChaosEvent() {{
            setDescription("Deleting chaos-test-dummy");
            setEngine("kubernetes");
            setOperation("delete");
            setTarget("chaos-test/chaos-test-dummy");
        }};
        Pod[] targets = runner.getTargets(ev);
        runner.deletePod(targets);
    }

    private static void updateCollector(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClass().getCanonicalName();
        System.out.println(callingClassName + " — " + type.toString() + " — " + String.join(" ", messages));
    }
}
