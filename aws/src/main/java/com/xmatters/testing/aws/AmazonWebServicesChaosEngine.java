package com.xmatters.testing.aws;

import java.util.Map;
import java.util.function.Consumer;

import software.amazon.awssdk.services.ec2.model.Ec2Exception;

import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.annotations.OperationName;
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

    @OperationName("delete")
    public void deleteVm(String[] targets) {
        tryForEachTarget(targets, "Deleting instance:", target -> connector.delete(target));
    }

    @OperationName("stop")
    public void stopVm(String[] targets) {
        tryForEachTarget(targets, "Stopping instance:", target -> connector.stop(target));
    }

    private void tryForEachTarget(String[] targets, String message, Consumer<String> action) {
        for (String target : targets) {
            this.sendUpdate(UpdateType.INFO, message, target);
            try {
                action.accept(target);
            } catch (Ec2Exception e) {
                this.sendUpdate(UpdateType.ERROR, e.toString());
            }
        }
    }
}
