package com.xmatters.testing.cthulhu;

import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.api.scenario.Scenario;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import com.xmatters.testing.cthulhu.eventrunner.ChaosEventScheduler;
import lombok.extern.slf4j.Slf4j;
import me.atrox.haikunator.Haikunator;
import me.atrox.haikunator.HaikunatorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@SpringBootApplication
public class Cthulhu implements CommandLineRunner {

    private static final String APPLICATION_NAME = "Cthulhu";
    private static final String CONFIG_APPLICATION_NAME1 = "application.name";
    private static final String CONFIG_RUN_NAME = "run.name";

    @Autowired(required = false)
    private Scenario scenario;

    @Autowired
    private ChaosEventScheduler scheduler;

    @Autowired
    private AuditingService auditingService;

    @Value("${run.name}")
    private String runName;

    public static void main(String[] args) {
        setupRunProperties();

        SpringApplication.run(Cthulhu.class, args);
    }

    /**
     * Declares properties for the run.
     */
    private static void setupRunProperties() {
        Haikunator haikunator = new HaikunatorBuilder().setTokenLength(0).build();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        System.setProperty(CONFIG_RUN_NAME, dateFormat.format(new Date()) + "-" + haikunator.haikunate());
        System.setProperty(CONFIG_APPLICATION_NAME1, APPLICATION_NAME);
    }

    @Override
    public void run(String... args) {
        log.info("RunID: {}", runName);

        if(scenario == null) {
            log.error("No scenario was provided.  Scenario can be passed in with the '--scenario argument' or with STDIN.");
            return;
        }

        log.info("Playing scenario '{}'.", scenario.getName());
        auditingService.publishEvent(ChaosAuditType.CTHULHU_STARTED, scenario.getName());
        for (ChaosEvent ev : scenario.getChaosEvents()) {
            scheduler.schedule(ev);
        }
        scheduler.waitForEnd();
        auditingService.publishEvent(ChaosAuditType.CTHULHU_ENDED, scenario.getName());
        log.info("Scenario '{}' ended.", scenario.getName());
    }
}
