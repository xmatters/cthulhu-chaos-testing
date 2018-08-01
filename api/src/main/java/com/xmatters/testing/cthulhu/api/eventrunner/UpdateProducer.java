package com.xmatters.testing.cthulhu.api.eventrunner;

/**
 * Define an interface for modules to send execution updates to Cthulhu.
 */
public interface UpdateProducer {

    /**
     * Send execution updates to Cthulhu, which are then forwarded to any ChaosAuditors registered.
     *
     * @param type Type of update to send.
     * @param messages The update message(s) to send.
     */
    void sendUpdate(UpdateType type, String... messages);
}
