package com.xmatters.testing.cthulhu.auditing;

import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AuditingService {

    private List<ChaosAuditor> auditors = new ArrayList<>();

    public void publishEvent(ChaosAuditType auditType, String... message) {
        auditors.forEach(a -> {
            try {
                a.processEvent(auditType, message);
            } catch (Exception e) {
                log.error(e.toString());
            }
        });
    }

    public void registerChaosAuditor(ChaosAuditor auditor) {
        auditors.add(auditor);
    }
}
