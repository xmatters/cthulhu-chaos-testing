package com.xmatters.testing.cthulhu.eventrunner;

import com.xmatters.testing.cthulhu.api.annotations.EngineName;
import com.xmatters.testing.cthulhu.api.annotations.OperationName;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.eventrunner.ChaosEngine;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Runs a ChaosEngine/Operation with a given set of targets, based on the specifications of a ChaosEvent.
 */
@Slf4j
public class ChaosEventHandler {

    private ChaosEngine runnerInstance;
    private Method handlerMethod;
    private AuditingService auditingService;
    private String engineName;
    private String operationName;

    public ChaosEventHandler(ChaosEngine runnerInstance, Method handlerMethod, AuditingService auditingService) {
        EngineName engineName = runnerInstance.getClass().getAnnotation(EngineName.class);
        OperationName opName = handlerMethod.getAnnotation(OperationName.class);

        this.runnerInstance = runnerInstance;
        this.handlerMethod = handlerMethod;
        this.auditingService = auditingService;
        this.engineName = engineName.value();
        this.operationName = opName.value();
    }

    public boolean canHandle(ChaosEvent ev) {
        return engineName.equals(ev.getEngine()) && operationName.equals(ev.getOperation());
    }

    public void run(ChaosEvent ev) {
        try {
            handlerMethod.invoke(runnerInstance, new Object[]{getTargets(ev)});
        } catch (ReflectiveOperationException e) {
            log.error("Could not run the event '{}'.\n{}", ev.getDescription(), e);
            auditingService.publishEvent(ChaosAuditType.EVENT_ERROR, ev.getDescription(), e.toString());
        }
    }

    private Object[] getTargets(ChaosEvent ev) {
        Object[] targets = runnerInstance.getTargets(ev);

        if (targets == null || targets.length == 0) {
            log.warn("No target matching the event criteria.");
            return targets;
        }

        List<Object> list = Arrays.asList(targets);
        Collections.shuffle(list);

        int skip = (ev.getSkip() > 0) ? ev.getSkip() : 0;
        int limit = (ev.getQuantity() > 0) ? ev.getQuantity() : list.size();
        list = list.stream().skip(skip).limit(limit).collect(Collectors.toList());

        targets = Arrays.copyOf(targets, list.size());
        targets = list.toArray(targets);
        if (targets.length < limit) {
            log.warn("There are less target matches ({}) then the quantity specified ({}).", targets.length);
        }

        return targets;
    }
}
