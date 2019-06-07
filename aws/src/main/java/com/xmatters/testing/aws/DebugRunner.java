package com.xmatters.testing.aws;

import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

public class DebugRunner {

    public static void main(String[] args) throws Exception {
        AmazonWebServicesChaosEngine runner = new AmazonWebServicesChaosEngine();
        runner.setUpdateCollector(DebugRunner::updateCollector);
        runner.configure();

        String[] targets = runner.getTargets( new ChaosEvent() {{
            setDescription("Retrieve chaos-test-dummy instance id.");
            setEngine("aws-ec2");
            setTarget(".*/chaos-test-dummy");
        }});

        updateCollector(UpdateType.INFO, targets);
    }

    private static void updateCollector(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClass().getCanonicalName();
        System.out.println(callingClassName + " — " + type.toString() + " — " + String.join(" ", messages));
    }
}
