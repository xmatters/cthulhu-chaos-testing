package com.xmatters.testing.cthulhu.exceptions;

import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import lombok.Getter;

public class ChaosEventHandlerException extends RuntimeException {

    @Getter
    private String reason;

    @Getter
    private String engine;

    @Getter
    private String operation;

    public ChaosEventHandlerException(String reason, ChaosEvent ev) {
        this(reason, ev.getEngine(), ev.getOperation());
    }

    public ChaosEventHandlerException(String reason, String engine, String operation) {
        super(reason + " for the operation '" + operation + "' on the engine '" + engine + "'.");
        this.reason = reason;
        this.engine = engine;
        this.operation = operation;
    }
}
