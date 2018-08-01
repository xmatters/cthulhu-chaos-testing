package com.xmatters.testing.cthulhu.eventrunner;

import com.xmatters.testing.cthulhu.api.annotations.OperationName;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateProducer;
import com.xmatters.testing.cthulhu.api.eventrunner.UpdateType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import com.xmatters.testing.cthulhu.exceptions.ChaosEventHandlerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Find and setup ChaosEngine/Operation handlers;  Dispatch ChaosEvents to the appropriate handler.
 */
@Slf4j
@Component
public class ChaosEventRunner {
    private static final int CALLING_CLASS_DISTANCE = 4;

    @Autowired
    private AuditingService auditingService = new AuditingService();

    @Autowired
    private Set<Class<? extends ChaosEngine>> chaosEngines;

    @Autowired
    @Qualifier("configuration")
    private Map<String, String> config = new HashMap<>();

    private List<ChaosEventHandler> handlers = new ArrayList<>();
    private UpdateProducer updateCollector = (UpdateType t, String... m) -> collectUpdates(t, m);

    public ChaosEventRunner() {
    }

    public ChaosEventRunner(AuditingService auditingService, Map<String, String> config) {
        this.auditingService = auditingService;
        this.config = config;
    }

    @PostConstruct
    private void buildHandlerList() {
        loadOperationsFromChaosEngines(chaosEngines);
    }

    @SuppressWarnings("unchecked")
    public void loadOperationsFromChaosEngines(Set<Class<? extends ChaosEngine>> chaosEngines) {
        for (Class runnerClass : chaosEngines) {
            Class targetType = getTargetClass(runnerClass);
            ChaosEngine runnerInstance = getChaosEngineInstance(runnerClass);

            if (targetType == null || runnerInstance == null) {
                continue;
            }
            log.info("Loading {}", runnerClass.getCanonicalName());

            handlers.addAll(Stream.of(runnerClass.getMethods())
                    .filter(m -> isMethodChaosOperation(m, targetType))
                    .map(handlerMethod -> new ChaosEventHandler(runnerInstance, handlerMethod, auditingService))
                    .collect(Collectors.toList()));
        }
    }

    public void execute(ChaosEvent ev) {
        List<ChaosEventHandler> candidates = handlers.stream()
                .filter(h -> h.canHandle(ev))
                .collect(Collectors.toList());

        if (candidates.size() == 0) {
            throw new ChaosEventHandlerException("No candidates", ev);
        }

        if (candidates.size() > 1) {
            throw new ChaosEventHandlerException("Too many candidates", ev);
        }

        auditingService.publishEvent(ChaosAuditType.EVENT_RUNNING, ev.getDescription());
        candidates.get(0).run(ev);
        auditingService.publishEvent(ChaosAuditType.EVENT_ENDED, ev.getDescription());
    }

    private Class getTargetClass(Class<? extends ChaosEngine> runnerClass) {
        try {
            return runnerClass.getMethod("getTargets", ChaosEvent.class).getReturnType();
        } catch (NoSuchMethodException e) {
            // We can get here if the jar file containing the runner is missing dependencies.
            log.error(
                    "Could not detect the target type of the Chaos Engine {}.  Are you sure the jar file is including all its dependencies?\n{}",
                    runnerClass.getCanonicalName(),
                    e.toString()
            );
        }

        return null;
    }

    private ChaosEngine getChaosEngineInstance(Class<? extends ChaosEngine> engineClass) {
        try {
            ChaosEngine engine = engineClass.getConstructor().newInstance();
            engine.setConfiguration(config);
            engine.setUpdateCollector(updateCollector);
            engine.configure();
            return engine;
        } catch (NoSuchMethodException e) {
            log.error("There is no default constructor for the Chaos Engine {}.", engineClass.getCanonicalName());
        } catch (Exception e) {
            log.error("Unable to get an instance of the Chaos Engine {}.\n {}", engineClass.getCanonicalName(), e.toString());
        }

        return null;
    }

    private static boolean isMethodChaosOperation(Method m, Class chaosEventTargetType) {
        boolean isOperation = m.getAnnotation(OperationName.class) != null;

        if (!isOperation) {
            return false;
        }

        Parameter[] params = m.getParameters();
        isOperation = isOperation && params.length == 1;
        isOperation = isOperation && params[0].getType().isAssignableFrom(chaosEventTargetType);

        if (!isOperation) {
            log.warn(
                    "{}.{} does not match the OperationName method interface.  Expected `void operation({}[])`.",
                    m.getDeclaringClass().getCanonicalName(),
                    m.getName(),
                    chaosEventTargetType.getSimpleName()
            );
        }

        return isOperation;
    }

    private void collectUpdates(UpdateType type, String... messages) {
        String callingClassName = Thread.currentThread().getStackTrace()[CALLING_CLASS_DISTANCE].getClassName();
        org.slf4j.Logger reflectedLogger = org.slf4j.LoggerFactory.getLogger(callingClassName);

        String message = String.join(" ", messages);
        switch (type) {
            case DEBUG:
                reflectedLogger.debug(message);
                break;
            case INFO:
                reflectedLogger.info(message);
                auditingService.publishEvent(ChaosAuditType.EVENT_UPDATE, messages);
                break;
            case WARNING:
                reflectedLogger.warn(message);
                auditingService.publishEvent(ChaosAuditType.EVENT_WARNING, messages);
                break;
            case ERROR:
                reflectedLogger.error(message);
                auditingService.publishEvent(ChaosAuditType.EVENT_ERROR, messages);
        }
    }
}



