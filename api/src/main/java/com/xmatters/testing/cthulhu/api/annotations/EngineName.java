package com.xmatters.testing.cthulhu.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assign a name to a ChaosEngine.  Chaos Events, in Scenario files use that name in the `engine` field, in combination
 * with the `operation` field to tell Cthulhu where to find the implementation for the event to run.  It is possible for
 * multiple ChaosEngine to share the same EngineName, as long as the Operation Name they define do not overlap.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EngineName {

    /**
     * The name of the EngineName.  Matches the ChaosEvent `engine` field.
     */
    String value();
}
