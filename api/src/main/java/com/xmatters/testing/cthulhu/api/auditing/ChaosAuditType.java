package com.xmatters.testing.cthulhu.api.auditing;

/**
 * Types of Audit sent by Cthulhu.
 */
public enum ChaosAuditType {
    CTHULHU_STARTED,
    EVENT_SCHEDULED,
    EVENT_RUNNING,
    EVENT_UPDATE,
    EVENT_WARNING,
    EVENT_ERROR,
    EVENT_ENDED,
    CTHULHU_ENDED,
}
