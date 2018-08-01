package com.xmatters.testing.cthulhu.eventrunner;

import com.xmatters.testing.cthulhu.api.auditing.ChaosAuditType;
import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.api.scenario.Duration;
import com.xmatters.testing.cthulhu.api.scenario.Schedule;
import com.xmatters.testing.cthulhu.auditing.AuditingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Schedule ChaosEvents for execution.
 */
@Slf4j
@Service
public class ChaosEventScheduler {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Autowired
    private AuditingService auditingService = new AuditingService();

    @Autowired
    private ChaosEventRunner runner;

    @Value("cthulhu.event.timeout.default:5m")
    private Duration defaultTimeout = new Duration("5m");

    private final long baseDate = new Date().getTime();
    private List<Thread> scheduledEvents;

    public ChaosEventScheduler() {
        scheduledEvents = new ArrayList<>();
    }

    public ChaosEventScheduler(ChaosEventRunner customRunner, List<Thread> threadPool, AuditingService auditingService) {
        runner = customRunner;
        scheduledEvents = threadPool;
        this.auditingService = auditingService;
    }

    /**
     * Schedule one or more instance of a ChaosEvent, based on the ChaosEvent's schedule (if provided).
     *
     * @param ev A ChaosEvent to schedule.
     */
    public void schedule(ChaosEvent ev) {
        Schedule schedule = ev.getSchedule();
        int repeat = schedule.getRepeat();
        long eventDelay = schedule.getInitialDelay().getMilliseconds();

        while (repeat-- > 0) {
            scheduledEvents.add(scheduleEventInstance(ev, new Date(baseDate + eventDelay)));
            eventDelay += schedule.getDelay().getMilliseconds();
        }
    }

    /**
     * Schedule a single ChaosEvent instance.  Handles ChaosEvent's delay, if defined.
     *
     * @param ev A ChaosEvent to schedule.
     * @param executionTime Time at which the ChaosEvent should start.
     * @return A Thread to the scheduled ChaosEvent.
     */
    private Thread scheduleEventInstance(ChaosEvent ev, Date executionTime) {
        Thread eventWithTimeout = new Thread(() -> {
            try {
                Thread.sleep(executionTime.getTime() - baseDate);
                runEventWithTimeout(ev);
            } catch (InterruptedException i) {
                log.warn("{} was interrupted before it could start.", ev.getDescription());
                auditingService.publishEvent(ChaosAuditType.EVENT_WARNING, ev.getDescription(), "timed out before it could start.");
            }
        });
        eventWithTimeout.setUncaughtExceptionHandler((t, e) -> this.handleUncaughtException(ev, e));
        eventWithTimeout.start();

        log.info("{} is scheduled for {}", ev.getDescription(), dateFormat.format(executionTime));
        auditingService.publishEvent(ChaosAuditType.EVENT_SCHEDULED, ev.getDescription(), executionTime.toString());
        return eventWithTimeout;
    }

    /**
     * Run a ChaosEvent; Interrupt it if its execution continues past the ChaosEvent's timeout (or the default timeout).
     *
     * @param ev A ChaosEvent to execute.
     * @throws InterruptedException
     */
    private void runEventWithTimeout(ChaosEvent ev) throws InterruptedException {
        Thread event = new Thread(() -> runner.execute(ev));
        event.setUncaughtExceptionHandler((t, e) -> this.handleUncaughtException(ev, e));
        event.start();
        event.join(ev.getTimeout(defaultTimeout).getMilliseconds());

        if (event.isAlive()) {
            log.warn("{} is running over time and will be interrupted.", ev.getDescription());
            auditingService.publishEvent(ChaosAuditType.EVENT_WARNING, ev.getDescription(), "is running over time and will be interrupted.");
            event.interrupt();
        } else {
            log.info("{} completed.", ev.getDescription());
        }
    }

    /**
     * Catches Exceptions thrown by the ChaosEvent execution within a thread.
     *
     * @param ev The ChaosEvent being executed.
     * @param e The Exception being thrown.
     */
    private void handleUncaughtException(ChaosEvent ev, Throwable e) {
        log.error("An error occurred while running {}. {}", ev.getDescription(), e.toString());
        auditingService.publishEvent(ChaosAuditType.EVENT_ERROR, ev.getDescription(), e.toString());
    }

    /**
     * Blocks execution until all ChaosEvents' Threads complete.
     */
    public void waitForEnd() {
        scheduledEvents.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                log.error(e.toString());
            }
        });
    }
}