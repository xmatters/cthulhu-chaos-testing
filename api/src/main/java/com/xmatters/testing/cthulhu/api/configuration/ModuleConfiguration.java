package com.xmatters.testing.cthulhu.api.configuration;

import java.util.Map;

/**
 * ChaosAuditors can extend from this class to gain access to Cthulhu's configuration elements.  ChaosEngines extend
 * from it by design, but do not need to use the configuration.
 *
 * The configuration elements are made of Cthulhu's property files, command line parameters and environment variables.
 * The name of environment variables are converted as follow: SOME_ENV_VAR -> some.env.var
 */
abstract public class ModuleConfiguration {

    private Map<String, String> config;

    /**
     * Called after an instance of ChaosAuditor or ChaosEngine is created by Cthulhu.  Override this method to define
     * custom configuration logic that must run before Cthulhu start executing its scenario.
     *
     * @throws Exception When the module is not able to configure itself to an operational state.  This causes Cthulhu
     * to ignore that module in the rest of its execution.
     */
    public void configure() throws Exception {
    }

    /**
     * Used to define or replace the configuration elements.
     *
     * @param configuration
     */
    public final void setConfiguration(Map<String, String> configuration) {
        config = configuration;
    }

    /**
     * Retrieve a configuration element.
     *
     * @param key The key of a configuration element.
     * @return The value associated to the key provided, or null if it is not defined.
     */
    public final String getConfigValue(String key) {
        return getConfigValue(key, null);
    }

    /**
     * Retrieve a configuration element.
     *
     * @param key The key of a configuration element.
     * @param defaultValue The value to return if the key is not defined.
     * @return The value associated to the key provided, or the defaultValue if the key is not defined.
     */
    public final String getConfigValue(String key, String defaultValue) {
        String value = config.get(key);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }
}
