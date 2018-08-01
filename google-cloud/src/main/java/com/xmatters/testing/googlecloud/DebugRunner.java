package com.xmatters.testing.googlecloud;

import com.google.api.services.compute.model.Instance;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DebugRunner {

    private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
        put("application.name", "cthulhu");
        put("run.name", "debug-run");

        // put("gcp.account.json", "/path/to/google-credentials.json");
        // put("gcp.project", gcp-project-name");
    }};

    private static final Map<String, ChaosEvent> chaosEvents = new HashMap<String, ChaosEvent>() {{
        // Demonstrates how to affect a subset of VMs out of a pool of matches.
        put("deleteRandomTestVms", new ChaosEvent() {{
            setDescription("Deleting two random chaos-test-dummy");
            setEngine("gcp-compute");
            setOperation("delete");
            setTarget(".*/chaos-test-dummy");
            setQuantity(2);
        }});
    }};

    public static void main(String[] args) throws Exception {
        GoogleCloudChaosEngine runner = new GoogleCloudChaosEngine();
        runner.setConfiguration(CONFIG);
        runner.setUpdateCollector(DebugRunner::updateCollector);
        runner.configure();

        Stream.of(args).forEach(eventName -> {
            ChaosEvent ev = chaosEvents.getOrDefault(eventName, new ChaosEvent());
            Instance[] targets = runner.getTargets(ev);
            runner.stopVm(targets);
        });
    }

    private static void updateCollector(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClass().getCanonicalName();
        System.out.println(callingClassName + " — " + type.toString() + " — " + String.join(" ", messages));
    }
}
