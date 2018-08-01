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
        for (Instance t : targets) {
            this.sendUpdate(UpdateType.INFO, "Deleting instance:", t.getName());
            try {
                connector.deleteInstance(t);
            } catch (IOException e) {
                this.sendUpdate(UpdateType.ERROR, e.toString());
            }
        }
    }

    @OperationName("reset")
    public void resetVm(Instance[] targets) {
        for (Instance t : targets) {
            this.sendUpdate(UpdateType.INFO, "Resetting instance:", t.getName());
            try {
                connector.resetInstance(t);
            } catch (IOException e) {
                this.sendUpdate(UpdateType.ERROR, e.toString());
            }
        }
    }

    @OperationName("stop")
    public void stopVm(Instance[] targets) {
        for (Instance t : targets) {
            this.sendUpdate(UpdateType.INFO, "Stopping instance:", t.getName());
            try {
                connector.stopInstance(t);
            } catch (IOException e) {
                this.sendUpdate(UpdateType.ERROR, e.toString());
            }
        }
    }
}
