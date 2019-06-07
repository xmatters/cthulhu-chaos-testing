package com.xmatters.testing.aws;

import java.util.Map;

import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

@EngineName("aws-ec2")
public class AmazonWebServicesChaosEngine extends ChaosEngine {

    private static final String TARGET_ZONE = "zone";
    private static final String TARGET_INSTANCE_NAME = "instanceName";

    private AWSConnector connector;

    @Override
    public void configure() throws Exception {
        super.configure();
        this.connector = new AWSConnector();
    }

    @Override
    public String[] getTargets(ChaosEvent ev) {
        Map<String, String> targetParts = ev.getTargetParts(TARGET_ZONE, TARGET_INSTANCE_NAME);
        try {
            return connector.findInstances(targetParts.get(TARGET_ZONE), targetParts.get(TARGET_INSTANCE_NAME));
        } catch (Exception e) {
            this.sendUpdate(UpdateType.ERROR, "An error occurred while searching for instances.", e.toString());
        }
        return null;
    }
}
