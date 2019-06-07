package com.xmatters.testing.googlecloud;

import com.google.api.services.compute.model.Instance;
import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.annotations.OperationName;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

import java.io.IOException;
import java.util.Map;

@EngineName("gcp-compute")
public class GoogleCloudChaosEngine extends ChaosEngine {

    private static final String TARGET_ZONE = "zone";
    private static final String TARGET_INSTANCE = "instance";

    private CloudConnector connector;

    @Override
    public void configure() throws Exception {
        super.configure();
        connector = new CloudConnector(this);
    }

    @Override
    public Instance[] getTargets(ChaosEvent ev) {
        Map<String, String> targetParts = ev.getTargetParts(TARGET_ZONE, TARGET_INSTANCE);
        try {
            return connector.findInstances(targetParts.get(TARGET_ZONE), targetParts.get(TARGET_INSTANCE));
        } catch (Exception e) {
            this.sendUpdate(UpdateType.ERROR, "An error occurred while searching for instances.", e.toString());
        }

        return null;
    }

    @OperationName("delete")
    public void deleteVm(Instance[] targets) {
        tryForEachTarget(targets, "Deleting instance:", t -> connector.deleteInstance(t));
    }

    @OperationName("reset")
    public void resetVm(Instance[] targets) {
        tryForEachTarget(targets, "Resetting instance:", t -> connector.resetInstance(t));
    }

    @OperationName("stop")
    public void stopVm(Instance[] targets) {
        tryForEachTarget(targets, "Stopping instance:", t -> connector.stopInstance(t));
    }

    private void tryForEachTarget(Instance[] targets, String message, ThrowingConsumer<Instance> action) {
        for (Instance target : targets) {
            this.sendUpdate(UpdateType.INFO, message, target.getName());
            try {
                action.accept(target);
            } catch (IOException e) {
                this.sendUpdate(UpdateType.ERROR, e.toString());
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }
}
