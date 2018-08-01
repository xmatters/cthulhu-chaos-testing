package com.xmatters.testing.cthulhu.api.eventrunner;

import com.xmatters.testing.cthulhu.api.configuration.ModuleConfiguration;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;

/**
 * A ChaosEngine defines Operations, which can be used in ChaosEvents.  ChaosEngines must have an @EngineName annotation,
 * which ChaosEvents use inthe `engine` field along with the `operation` field to select which method will handle them.
 *
 * ChaosEngines can share a single EngineName, as long as the name each operation is unique.
 */
abstract public class ChaosEngine extends ModuleConfiguration implements UpdateProducer {

    private UpdateProducer updateCollector;

    /**
     * Called before running a ChaosEvent.  This method should return the full list of candidates that can be affected
     * by the event — presumably based on the ChaosEvent's `target` field, or its helper method `getTargetParts()`).
     * Further filtering and restriction from `quantity` and `skip` is applied on the resulting array of possibilities.
     *
     * When overriding, it is usual to change the Type of array returned.  This Type has to match the input parameter of
     * Operation methods defined within this ChaosEngine.
     *
     * @param ev A Chaos Event that is scheduled to run.
     * @return A complete list of candidates that can be affected by the given ChaosEvent.
     */
    abstract public Object[] getTargets(ChaosEvent ev);

    /**
     * Used by Cthulhu during the instantiation of a ChaosEngine to connect the updates to the ChaosAuditors.
     * @param updateCollector
     */
    public final void setUpdateCollector(UpdateProducer updateCollector) {
        this.updateCollector = updateCollector;
    }

    @Override
    public final void sendUpdate(UpdateType type, String... messages) {
        updateCollector.sendUpdate(type, messages);
    }

}
