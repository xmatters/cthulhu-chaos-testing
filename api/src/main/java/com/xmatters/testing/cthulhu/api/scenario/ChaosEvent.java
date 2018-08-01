package com.xmatters.testing.cthulhu.api.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A distinct chaos event, targeted at specific entities in the system under test.
 */
public class ChaosEvent {

    /**
     * Description of what this event does.
     */
    @Getter @Setter
    @JsonProperty(required = true)
    private String description;

    /**
     * Name of the Engine used for this event (eg. [kubernetes, gcp-compute, ...]).
     */
    @Getter @Setter
    @JsonProperty(required = true)
    private String engine;

    /**
     * Name of the operation to be performed.
     */
    @Getter @Setter
    @JsonProperty(required = true)
    private String operation;

    /**
     * Operation-specific parameters.
     */
    @Getter @Setter
    private Map<String, String> parameters;

    /**
     * Restricts the number of targets used in the event (if more than one).
     */
    @Getter @Setter
    private int quantity;

    /**
     * Skips a number of targets to ensure that amount is never affected.
     */
    @Getter @Setter
    private int skip;

    /**
     * Event scheduling parameters.
     */
    @Setter
    private Schedule schedule;

    /**
     * Target(s) for the Operation.  Supports regex.
     * Eg. kube-namespace/pod-name, gcp-zone/vm-name
     */
    @Setter
    @JsonProperty(required = true)
    private String target;

    /**
     * Delay before cancelling the event (in case an event waits on resources that are unavailable).
     * Overrides the global setting job.timeout.default
     */
    @Setter
    private Duration timeout;

    /**
     * The target network interface (for operations applied to network interfaces).
     *
     * @deprecated Use the Target field with the format: "target/interface", instead.
     */
    @Deprecated
    @Getter @Setter
    @JsonProperty("interface")
    private String ifx;

    /**
     * Namespace or Zone containing an instance.
     *
     * @deprecated Use the Target field with the format: "sector/target", instead.
     */
    @Deprecated
    @Getter @Setter
    private String sector;

    /**
     * Get the execution schedule for the Chaos Event.
     *
     * @return The execution schedule for the Chaos Event.
     */
    public Schedule getSchedule() {
        if (schedule == null) {
            schedule = new Schedule();
        }

        return schedule;
    }

    /**
     * Get the Chaos Event target parts (which provided in the `target` field separated by `/`).
     *
     * @param partNames Names for the different target parts.
     * @return A map of targetPartName/value.
     */
    public Map<String, String> getTargetParts(String... partNames) {
        List<String> targetParts = new ArrayList<>(Arrays.asList(target.split("/")));

        if (!Strings.isNullOrEmpty(sector)) {
            targetParts.add(0, sector);
        }

        if (!Strings.isNullOrEmpty(ifx)) {
            targetParts.add(ifx);
        }

        return targetParts.stream()
                .limit(partNames.length)
                .reduce(new HashMap<String, String>(), (map, s) -> {
                    map.put(partNames[map.size()], s);
                    return map;
                }, (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
    }

    /**
     * Get the ChaosEvent timeout after which Cthulhu will kill the event's execution.
     *
     * @param defaultTimeout Timeout to use if the ChaosEvent does not define any.
     * @return The timeout after which Cthulhu will kill the event's execution.
     */
    public Duration getTimeout(Duration defaultTimeout) {
        return timeout == null ? defaultTimeout : timeout;
    }
}
