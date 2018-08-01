package com.xmatters.testing.cthulhu.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditor;
import com.xmatters.testing.cthulhu.api.configuration.ModuleConfiguration;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.scenario.Scenario;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Configuration
public class ComponentConfiguration {

    private static final Pattern CONFIG_SUBSTITUTION = Pattern.compile("\\$\\{(.*)}");
    private static final String VALID_RESOURCES_FILES = ".*\\.(jar)$";
    private static final String LOCAL_MODULE_PACKAGE = "com.xmatters.testing";

    @Autowired
    Environment env;

    @Value("${scenario:}")
    private String scenarioFilePath;

    @Value("${cthulhu.use-external-modules:false}")
    private boolean useExternalModules = false;

    @Value("${cthulhu.module.paths:./modules}")
    private String modulePaths;

    private InputStream scenarioStream;
    private Reflections cachedReflector;

    public ComponentConfiguration() {
        this(System.in);
    }

    public ComponentConfiguration(InputStream input) {
        scenarioStream = input;
    }

    @Bean
    public Scenario getScenario() {
        if (!Strings.isNullOrEmpty(scenarioFilePath)) {
            File scenarioFile = new File(scenarioFilePath);
            try {
                scenarioStream = new FileInputStream(scenarioFile);
            } catch (FileNotFoundException e) {
                log.error("The scenario file '{}' does not exist.", scenarioFilePath);
            }
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            if (scenarioStream.available() > 0) {
                log.info("Parsing Scenario.");
                return mapper.readValue(scenarioStream, Scenario.class);
            }
        } catch (IOException e) {
            log.error("Could not read the scenario.", e);
        }

        return null;
    }

    @Bean("configuration")
    public Map<String, String> getConfiguration() {
        Map<String, String> configMap = new HashMap<>();
        for (PropertySource<?> propertySource : ((AbstractEnvironment) env).getPropertySources()) {
            if (propertySource instanceof SimpleCommandLinePropertySource) {
                SimpleCommandLinePropertySource source = (SimpleCommandLinePropertySource) propertySource;
                Arrays.stream(source.getPropertyNames()).forEach(key -> configMap.put(key, source.getProperty(key)));
            }

            if (propertySource instanceof MapPropertySource) {
                Map<String, Object> properties = ((MapPropertySource) propertySource).getSource();
                if(propertySource.getName().equals("systemEnvironment")) {
                    properties.forEach((s, o) -> configMap.putIfAbsent(s.toLowerCase().replace('_', '.'), o.toString()));
                } else {
                    properties.forEach((s, o) -> configMap.putIfAbsent(s, o.toString()));
                }
            }
        }

        Map<String, String> history = new HashMap<>();
        configMap.replaceAll((key, value) -> getExpandedConfigValue(configMap, key, value, history));

        return configMap;
    }

    private String getExpandedConfigValue(final Map<String, String> lookup, String key, String value, Map<String, String> history) {
        if (value == null) {
            return null;
        }

        Matcher m = CONFIG_SUBSTITUTION.matcher(value);
        while (m.find()) {
            String placeholder = m.group(0);
            String configName = m.group(1);

            String replaceValue = null;
            if (history.containsKey(configName)) {
                replaceValue = history.get(configName);
            } else {
                if (!key.equals(configName)) {
                    replaceValue = lookup.get(configName);
                    history.put(configName, replaceValue);
                    replaceValue = getExpandedConfigValue(lookup, configName, replaceValue, history);
                }
                history.put(configName, replaceValue);
            }

            if (replaceValue != null) {
                value = value.replace(placeholder, replaceValue);
            }
        }

        return value;
    }

    @Bean
    public Set<Class<? extends ChaosEngine>> getChaosEngines() {
        Reflections reflections = getModuleReflector();

        return reflections.getSubTypesOf(ChaosEngine.class).stream()
                .filter(clazz -> {
                    EngineName engineName = clazz.getAnnotation(EngineName.class);
                    boolean isValid = engineName != null && !Strings.isNullOrEmpty(engineName.value());

                    if (!isValid) {
                        log.warn("Class {} derives is a {} but does not have an @{} attribute.", clazz.getCanonicalName(), ChaosEngine.class.getSimpleName(), EngineName.class.getSimpleName());
                    }

                    return isValid;
                })
                .collect(Collectors.toSet());
    }

    @Bean
    public AuditingService getChaosAuditors() {
        Reflections reflections = getModuleReflector();
        Map<String, String> config = getConfiguration();

        AuditingService auditingService = new AuditingService();
        reflections.getSubTypesOf(ChaosAuditor.class).stream()
                .map(clazz -> getAuditorInstance(clazz, config))
                .filter(Objects::nonNull)
                .forEach(auditor -> {
                    auditingService.registerChaosAuditor(auditor);
                    log.info("Loading {}", auditor.getClass().getCanonicalName());
                });

        return auditingService;
    }

    private Reflections getModuleReflector() {
        if (cachedReflector == null) {
            if(useExternalModules) {
                URL[] resources = Stream.of(modulePaths.split(File.pathSeparator))
                        .map(File::new).flatMap(f -> f.isDirectory() ? Stream.of(f.listFiles()) : Stream.of(f))
                        .filter(f -> !f.isDirectory() && f.getName().matches(VALID_RESOURCES_FILES))
                        .map(ComponentConfiguration::getFileUrl).toArray(URL[]::new);

                URLClassLoader loader = new URLClassLoader(resources, ClassLoader.getSystemClassLoader());
                cachedReflector = new Reflections(loader);
            } else {
                return new Reflections(LOCAL_MODULE_PACKAGE);
            }
        }

        return cachedReflector;
    }

    private static URL getFileUrl(File f) {
        try {
            return f.toURI().toURL();
        } catch (MalformedURLException e) {
            log.error(e.toString());
        }
        return null;
    }

    private ChaosAuditor getAuditorInstance(Class<? extends ChaosAuditor> auditorClass, Map<String, String> config) {
        try {
            ChaosAuditor auditor = auditorClass.getConstructor().newInstance();
            if (ModuleConfiguration.class.isAssignableFrom(auditorClass)) {
                ((ModuleConfiguration) auditor).setConfiguration(config);
                ((ModuleConfiguration) auditor).configure();
            }
            return auditor;
        } catch (NoSuchMethodException e) {
            log.error("There is no default constructor for the Chaos Auditor {}.", auditorClass.getCanonicalName());
        } catch (Exception e) {
            log.error("Unable to get an instance of the Chaos Auditor {}.\n {}", auditorClass.getCanonicalName(), e.toString());
        }

        return null;
    }
}
