package com.xmatters.testing.kubernetes;

import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.annotations.OperationName;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.Map;

@EngineName("kubernetes")
public class KubeChaosEngine extends ChaosEngine {

    private static final String TARGET_POD = "pod";
    private static final String TARGET_NAMESPACE = "namespace";
    private KubeConnector connector;

    @Override
    public void configure() throws Exception {
        super.configure();
        connector = new KubeConnector(this);
    }

    @Override
    public Pod[] getTargets(ChaosEvent ev) {
        Map<String, String> targetParts = ev.getTargetParts(TARGET_NAMESPACE, TARGET_POD);
        return connector.findPods(targetParts.get(TARGET_NAMESPACE), targetParts.get(TARGET_POD));
    }

    @OperationName("delete")
    public void deletePod(Pod[] targets) {
        for (Pod p: targets             ) {
            this.sendUpdate(UpdateType.INFO, "Deleting pod:", p.getMetadata().getName());

            connector.deletePod(targets);
        }
    }
}
