package com.xmatters.testing.cthulhu.api.scenario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.PrimitiveIterator;
import java.util.Random;

/**
 * An execution schedule that allow an event to execute with delay and/or to repeat a certain number
 * of times.
 */
public class Schedule {

    /**
     * A delay before the execution of an instance of ChaosEvent.  Defaults to 0s (immediate execution).
     */
    @Setter
    private Duration delay;

    /**
     * A range of variation that is added or removed from the delay.  Defaults to 0s (no variation).
     */
    @Setter
    @JsonProperty("delayjitter")
    private Duration delayJitter;

    /**
     * A delay before the execution of the first instance of a repeating ChaosEvent.  Defaults to the value set
     * on `delay`.
     */
    @Setter
    @JsonProperty("initialdelay")
    private Duration initialDelay;

    /**
     * Number of times that the event should repeat.
     */
    @Getter @Setter
    private int repeat = 1;

    private PrimitiveIterator.OfLong jitterGenerator;

    /**
     * Get the delay for an instance of ChaosEvent.
     *
     * @return A delay to wait before the execution of a ChaosEvent.
     */
    public Duration getDelay() {
        if (delay == null) {
            delay = new Duration();
        }

        return applyDelayJitter(delay);
    }

    /**
     * Get the delay for the first instance of ChaosEvent.
     *
     * @return A delay to wait before the execution of the first instance of a ChaosEvent.
     */
    public Duration getInitialDelay() {
        if (initialDelay == null) {
            if (delay == null) {
                delay = new Duration();
            }

            initialDelay = delay;
        }

        return applyDelayJitter(initialDelay);
    }

    private Duration applyDelayJitter(Duration base) {
        return new Duration(base.getMilliseconds() + getDelayJitter());
    }

    private long getDelayJitter() {
        if (delayJitter == null) {
            return 0;
        }

        if (jitterGenerator == null) {
            jitterGenerator = new Random().longs(0 - delayJitter.getMilliseconds(), delayJitter.getMilliseconds()).iterator();
        }

        return jitterGenerator.nextLong();
    }
}
