package com.xmatters.testing.kubernetes;

import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DebugRunner {

    private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
        put("application.name", "cthulhu");
        put("run.name", "debug-run");

        // put("kube.config", "/path/to/.kube/config");
    }};

    private static final Map<String, ChaosEvent> chaosEvents = new HashMap<String, ChaosEvent>(){{
        // Demonstrates how to affect a subset of Pods out of a pool of matches.
        put("destroyTestPod", new ChaosEvent() {{
            setDescription("Deleting two random chaos-test-dummy");
            setEngine("kubernetes");
            setOperation("delete");
            setTarget("chaos-test/chaos-test-dummy");
            setQuantity(2);
        }});
    }};

    public static void main(String[] args) throws Exception {
        KubeChaosEngine runner = new KubeChaosEngine();
        runner.setConfiguration(CONFIG);
        runner.setUpdateCollector(DebugRunner::updateCollector);
        runner.configure();

        Stream.of(args).forEach(eventName -> {
            ChaosEvent ev = chaosEvents.getOrDefault(eventName, new ChaosEvent());
            Pod[] targets = runner.getTargets(ev);
            runner.deletePod(targets);
        });
    }

    private static void updateCollector(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClass().getCanonicalName();
        System.out.println(callingClassName + " — " + type.toString() + " — " + String.join(" ", messages));
    }
}
