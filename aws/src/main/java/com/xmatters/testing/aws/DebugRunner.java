package com.xmatters.testing.aws;

import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

public class DebugRunner {

    public static void main(String[] args) throws Exception {
        AmazonWebServicesChaosEngine runner = new AmazonWebServicesChaosEngine();
        runner.setUpdateCollector(DebugRunner::updateCollector);
        runner.configure();

        // Demonstrates how to affect a subset of VMs out of a pool of matches.
        ChaosEvent ev =  new ChaosEvent() {{
            setDescription("Operate on chaos-test-dummy in AWS");
            setEngine("aws-ec2");
            setOperation("not-used");
            setTarget(".*/chaos-test-dummy");
        }};
        String[] targets = runner.getTargets(ev);
        runner.deleteVm(targets);
    }

    private static void updateCollector(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[2].getClass().getCanonicalName();
        System.out.println(callingClassName + " — " + type.toString() + " — " + String.join(" ", messages));
    }
}
