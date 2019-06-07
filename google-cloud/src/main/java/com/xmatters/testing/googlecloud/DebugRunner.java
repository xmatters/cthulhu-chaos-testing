package com.xmatters.testing.googlecloud;

import com.google.api.services.compute.model.Instance;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

import java.util.HashMap;
import java.util.Map;

public class DebugRunner {

    private static final Map<String, String> CONFIG = new HashMap<String, String>() {{
        put("application.name", "cthulhu");
        put("run.name", "debug-run");

        // put("gcp.account.json", "/path/to/google-credentials.json");
        // put("gcp.project", gcp-project-name");
    }};

    public static void main(String[] args) throws Exception {
        GoogleCloudChaosEngine runner = new GoogleCloudChaosEngine();
        runner.setConfiguration(CONFIG);
        runner.setUpdateCollector(DebugRunner::updateCollector);
        runner.configure();

        ChaosEvent ev = new ChaosEvent() {{
            setDescription("Operate on chaos-test-dummy in GCP");
            setEngine("gcp-compute");
            setOperation("not-used");
            setTarget(".*/chaos-test-dummy");
        }};
        Instance[] targets = runner.getTargets(ev);
        // runner.deleteVm(targets);
        // runner.stopVm(targets);
        runner.resetVm(targets);
    }

    private static void updateCollector(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClass().getCanonicalName();
        System.out.println(callingClassName + " — " + type.toString() + " — " + String.join(" ", messages));
    }
}
