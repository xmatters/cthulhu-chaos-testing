package com.xmatters.testing.cthulhu.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Assign an Operation name to a ChaosEngine's method.  Chaos Events, in Scenario files use that name in the `operation`
 * field, in combination with the `engine` field to tell Cthulhu where to find the implementation for the event to run.
 * It is possible for multiple ChaosEngine to share the same EngineName, as long as the Operation Name they define do
 * not overlap.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationName {

    /**
     * The name of the Operation.  Matches the ChaosEvent `operation` field.
     */
    String value();
}
