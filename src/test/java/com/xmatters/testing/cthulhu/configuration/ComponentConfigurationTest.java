package com.xmatters.testing.cthulhu.configuration;

import com.xmatters.testing.cthulhu.api.scenario.ChaosEvent;
import com.xmatters.testing.cthulhu.api.scenario.Scenario;
import com.xmatters.testing.cthulhu.api.scenario.Schedule;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

public class ComponentConfigurationTest {

    private ComponentConfiguration setupComponentConfiguration(String scenario) {
        return new ComponentConfiguration(new ByteArrayInputStream(scenario.getBytes()));
    }

    @Test
    public void parseFromStdIn() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: multipart/event/target\n" +
"    operation: the operation\n" +
"    quantity: 2").getScenario();

        Assert.assertEquals("Scenario Name", scn.getName());

        Assert.assertEquals(1, scn.getChaosEvents().length);
        ChaosEvent ev = scn.getChaosEvents()[0];
        Assert.assertEquals("Event Description", ev.getDescription());
        Assert.assertEquals("the engine", ev.getEngine());
        Assert.assertEquals("the operation", ev.getOperation());
        Assert.assertEquals(2, ev.getQuantity());

        Map<String, String> targetParts = ev.getTargetParts("placeholder", "name", "instance");
        Assert.assertEquals(3, targetParts.size());
        Assert.assertEquals("multipart", targetParts.get("placeholder"));
        Assert.assertEquals("event", targetParts.get("name"));
        Assert.assertEquals("target", targetParts.get("instance"));
    }

    @Test
    public void targetSubset() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: us-central1-a/someinstance/eth0\n" +
"    operation: the operation\n" +
"    quantity: 2").getScenario();

        ChaosEvent ev = scn.getChaosEvents()[0];
        Map<String, String> targetParts = ev.getTargetParts("zone", "instance");
        Assert.assertEquals(2, targetParts.size());
        Assert.assertEquals("us-central1-a", targetParts.get("zone"));
        Assert.assertEquals("someinstance", targetParts.get("instance"));
    }

    @Test
    public void targetCountValidation() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: us-central1-a/someinstance\n" +
"    operation: the operation\n" +
"    quantity: 2").getScenario();

        ChaosEvent ev = scn.getChaosEvents()[0];
        Map<String, String> targetParts = ev.getTargetParts("zone", "instance", "interface");
        Assert.assertEquals(2, targetParts.size());
        Assert.assertEquals("us-central1-a", targetParts.get("zone"));
        Assert.assertEquals("someinstance", targetParts.get("instance"));
    }

    @Test
    public void interfaceMappedToTarget() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    interface: eth0\n" +
"    operation: the operation\n" +
"    quantity: 2").getScenario();

        ChaosEvent ev = scn.getChaosEvents()[0];
        Map<String, String> targetParts = ev.getTargetParts("zone", "instance", "interface");
        Assert.assertEquals(3, targetParts.size());
        Assert.assertEquals("main", targetParts.get("zone"));
        Assert.assertEquals("target", targetParts.get("instance"));
        Assert.assertEquals("eth0", targetParts.get("interface"));
    }

    @Test
    public void sectorMappedToTarget() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    sector: us-central1-a\n" +
"    operation: the operation\n" +
"    quantity: 2").getScenario();

        ChaosEvent ev = scn.getChaosEvents()[0];
        Map<String, String> targetParts = ev.getTargetParts("zone", "placeholder", "interface");
        Assert.assertEquals(3, targetParts.size());
        Assert.assertEquals("us-central1-a", targetParts.get("zone"));
        Assert.assertEquals("main", targetParts.get("placeholder"));
        Assert.assertEquals("target", targetParts.get("interface"));
    }

    @Test
    public void defaultSchedule() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    interface: eth0\n" +
"    operation: the operation\n" +
"    quantity: 2").getScenario();

        Schedule sch = scn.getChaosEvents()[0].getSchedule();
        Assert.assertNotNull(sch);
        Assert.assertEquals(0, sch.getDelay().getMilliseconds());
        Assert.assertEquals(0, sch.getInitialDelay().getMilliseconds());
        Assert.assertEquals(1, sch.getRepeat());
    }

    @Test
    public void repeat() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    interface: eth0\n" +
"    operation: the operation\n" +
"    quantity: 2\n" +
"    schedule:\n" +
"        repeat: 5").getScenario();

        Schedule sch = scn.getChaosEvents()[0].getSchedule();
        Assert.assertEquals(5, sch.getRepeat());
    }

    @Test
    public void initialDelayDefaultsToDelay() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    interface: eth0\n" +
"    operation: the operation\n" +
"    quantity: 2\n" +
"    schedule:\n" +
"        delay: 1m").getScenario();

        Schedule sch = scn.getChaosEvents()[0].getSchedule();
        Assert.assertEquals(60000, sch.getDelay().getMilliseconds());
        Assert.assertEquals(60000, sch.getInitialDelay().getMilliseconds());
    }

    @Test
    public void initialDelayDifferentThanDelay() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    interface: eth0\n" +
"    operation: the operation\n" +
"    quantity: 2\n" +
"    schedule:\n" +
"        initialdelay: 2m\n" +
"        delay: 1m").getScenario();

        Schedule sch = scn.getChaosEvents()[0].getSchedule();
        Assert.assertEquals(60000, sch.getDelay().getMilliseconds());
        Assert.assertEquals(120000, sch.getInitialDelay().getMilliseconds());
    }

    @Test
    public void jitterAppliedToAllDelays() {
        Scenario scn = setupComponentConfiguration(
"name: Scenario Name\n" +
"description: Scenario Description\n" +
"chaosevents:\n" +
"  - description: Event Description\n" +
"    engine: the engine\n" +
"    target: main/target\n" +
"    interface: eth0\n" +
"    operation: the operation\n" +
"    quantity: 2\n" +
"    schedule:\n" +
"        initialdelay: 2m\n" +
"        delay: 1m\n" +
"        delayjitter: 5s").getScenario();

        Schedule sch = scn.getChaosEvents()[0].getSchedule();
        assertBetween(55000, 65000, sch.getDelay().getMilliseconds());
        assertBetween(115000, 125000, sch.getInitialDelay().getMilliseconds());
    }

    private void assertBetween(long minValue, long maxValue, long actualValue) {
        Assert.assertTrue("Expected " + actualValue + " to be greater or equal to " + minValue + ".", actualValue >= minValue);
        Assert.assertTrue("Expected " + actualValue + " to be less or equal to " + maxValue + ".", actualValue <= maxValue);
    }
}