package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.LbugList;
import com.ladybugdb.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for mapping LadybugDB Value objects to Java types.
 * Optimized to use direct Native-to-Java conversion via Value.getValue().
 */
public final class ValueMappers {

    private ValueMappers() {
        // Utility class
    }

    /**
     * Maps a Value to a String.
     *
     * @param value the LadybugDB Value
     * @return the string representation
     */
    public static String asString(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return value.getValue();
    }

    /**
     * Maps a Value to an Integer.
     *
     * @param value the LadybugDB Value
     * @return the integer value, or null if the value is null
     */
    public static Integer asInteger(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        Object raw = value.getValue();
        if (raw instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(raw.toString());
    }

    /**
     * Maps a Value to a Long.
     *
     * @param value the LadybugDB Value
     * @return the long value, or null if the value is null
     */
    public static Long asLong(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        Object raw = value.getValue();
        if (raw instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(raw.toString());
    }

    /**
     * Maps a Value to a Double.
     *
     * @param value the LadybugDB Value
     * @return the double value, or null if the value is null
     */
    public static Double asDouble(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        Object raw = value.getValue();
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(raw.toString());
    }

    /**
     * Maps a Value to a Boolean.
     *
     * @param value the LadybugDB Value
     * @return the boolean value, or null if the value is null
     */
    public static Boolean asBoolean(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return value.getValue();
    }

    /**
     * Maps a Value containing a list to a List of the specified type.
     * Bypasses string conversion where possible.
     */
    public static <T> List<T> asList(Value value, Function<Value, T> elementMapper) {
        if (value == null || value.isNull()) {
            return List.of();
        }

        try (LbugList lbugList = new LbugList(value)) {
            long size = lbugList.getListSize();
            List<T> result = new ArrayList<>((int) size);

            for (long i = 0; i < size; i++) {
                try (Value element = lbugList.getListElement(i)) {
                    result.add(elementMapper.apply(element));
                }
            }

            return result;
        }
    }

    public static List<String> asStringList(Value value) {
        return asList(value, Value::getValue);
    }

    public static List<Integer> asIntegerList(Value value) {
        return asList(value, ValueMappers::asInteger);
    }

    public static List<Long> asLongList(Value value) {
        return asList(value, ValueMappers::asLong);
    }

    public static List<Double> asDoubleList(Value value) {
        return asList(value, ValueMappers::asDouble);
    }

    public static List<Boolean> asBooleanList(Value value) {
        return asList(value, ValueMappers::asBoolean);
    }

    public static float[] asFloatArray(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        try (LbugList lbugList = new LbugList(value)) {
            long size = lbugList.getListSize();
            float[] result = new float[(int) size];

            for (long i = 0; i < size; i++) {
                try (Value element = lbugList.getListElement(i)) {
                    Object raw = element.getValue();
                    if (raw instanceof Number n) {
                        result[(int) i] = n.floatValue();
                    } else {
                        result[(int) i] = Float.parseFloat(raw.toString());
                    }
                }
            }

            return result;
        }
    }
}