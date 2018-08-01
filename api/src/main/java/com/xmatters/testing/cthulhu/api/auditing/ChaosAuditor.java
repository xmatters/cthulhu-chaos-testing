package com.xmatters.testing.cthulhu.api.auditing;

/**
 * A ChaosAuditor receives events from Cthulhu at during different parts of its execution.  This is provided to allow
 * the integration of Cthulhu with 3rd party applications.
 */
public interface ChaosAuditor {

    /**
     * Called for every AuditTypes sent by Cthulhu.
     *
     * @param auditType The type of Audit.
     * @param messages A message associated to the Audit.
     * @throws Exception
     */
    void processEvent(ChaosAuditType auditType, String... messages) throws Exception;
}
