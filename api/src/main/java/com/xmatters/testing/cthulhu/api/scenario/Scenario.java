package com.xmatters.testing.cthulhu.api.scenario;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines one or more ChaosEvents, that are to be executed by Cthulhu.
 */
@JsonIgnoreProperties({"authoremail", "description"})
public class Scenario {

    /**
     * Collection of ChaosEvents to execute in a given scenario.
     */
    @Getter @Setter
    @JsonProperty(value = "chaosevents", required = true)
    private ChaosEvent[] chaosEvents;

    /**
     * Name of the scenario.
     */
    @Getter @Setter
    @JsonProperty(required = true)
    private String name;
}
