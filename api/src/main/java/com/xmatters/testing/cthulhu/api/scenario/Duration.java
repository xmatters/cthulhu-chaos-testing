package com.xmatters.testing.cthulhu.api.scenario;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Longs;
import lombok.Getter;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A time duration.
 */
@JsonDeserialize(using = Duration.JsonDeserializer.class)
public class Duration {

    private static final Pattern DURATION_TOKEN = Pattern.compile("(\\d+)\\s*([a-z]*)", Pattern.CASE_INSENSITIVE);

    private static final Map<String, Number> VALUE_MAPPING = ImmutableMap.of(
            "h", 60 * 60 * 1000,
            "m", 60 * 1000,
            "s", 1000,
            "ms", 1
    );

    @Getter
    private long milliseconds = 0;

    /**
     * Default Duration.
     */
    public Duration() {
        this(0);
    }

    /**
     * A Duration defined in milliseconds.
     *
     * @param milliseconds Number of milliseconds in the Duration.
     */
    public Duration(long milliseconds) {
        this.milliseconds = milliseconds;
    }

    /**
     * A Duration defined in milliseconds.
     *
     * @param milliseconds Number of milliseconds in the Duration.
     */
    public Duration(Number milliseconds) {
        this(milliseconds.longValue());
    }

    /**
     * A Duration defined from a textual expression.
     *
     * @param expression Duration expression (1500ms, 1s 500ms, 1h 2m 3s 4ms, ...).
     */
    public Duration(String expression) {
        this(parseDurationExpression(expression));
    }

    private static long parseDurationExpression(String expression) {
        long duration = 0;
        Matcher m = DURATION_TOKEN.matcher(expression);

        while (m.find()) {
            long value = Longs.tryParse(m.group(1));
            String unit = m.group(2).toLowerCase();

            if (Strings.isNullOrEmpty(unit)) {
                duration += value;
            } else if (VALUE_MAPPING.containsKey(unit)) {
                duration += value * VALUE_MAPPING.get(unit).longValue();
            }
        }

        return duration;
    }

    /**
     * Deserializer to make Duration elements based on the value of a json field.
     */
    public static class JsonDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Duration> {

        public JsonDeserializer() {
            super();
        }

        @Override
        public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return p.getCurrentToken().isNumeric() ? new Duration(p.getNumberValue()) : new Duration(p.getText());
        }
    }
}
