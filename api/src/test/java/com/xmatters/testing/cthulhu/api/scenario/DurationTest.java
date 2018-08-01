package com.xmatters.testing.cthulhu.api.scenario;

import org.junit.Assert;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class DurationTest {

    @DataPoint
    public static final TestParams JUST_A_NUMBER = new TestParams("500", 500);

    @DataPoint
    public static final TestParams ONE_HUNDRED_MILLISECONDS = new TestParams("100ms", 100);

    @DataPoint
    public static final TestParams FORTY_SECONDS = new TestParams("40s", 40 * 1000);

    @DataPoint
    public static final TestParams THIRTY_SECONDS = new TestParams("30 S", 30 * 1000);

    @DataPoint
    public static final TestParams TWO_MINUTES = new TestParams("2m", 2 * 60 * 1000);

    @DataPoint
    public static final TestParams ONE_HOUR_TEN_MINUTES = new TestParams("1h 10 m", (1 * 60 + 10) * 60 * 1000);


    @Theory
    public void DurationIsParsedFromString(TestParams params) {
        Duration d = new Duration(params.entry);

        Assert.assertEquals(params.expectedValue, d.getMilliseconds());
    }

    private static class TestParams {
        String entry;
        long expectedValue;

        TestParams(String entry, long expectedValue) {
            this.entry = entry;
            this.expectedValue = expectedValue;
        }
    }

}