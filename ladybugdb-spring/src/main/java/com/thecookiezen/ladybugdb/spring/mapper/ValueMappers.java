package com.thecookiezen.ladybugdb.spring.mapper;

import com.ladybugdb.LbugList;
import com.ladybugdb.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for mapping LadybugDB Value objects to Java types.
 * Provides static methods for common type conversions to eliminate duplication
 * in RowMappers.
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
        return value.getValue().toString();
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
        Object rawValue = value.getValue();
        if (rawValue instanceof Integer i) {
            return i;
        }
        if (rawValue instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(rawValue.toString());
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
        Object rawValue = value.getValue();
        if (rawValue instanceof Long l) {
            return l;
        }
        if (rawValue instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(rawValue.toString());
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
        Object rawValue = value.getValue();
        if (rawValue instanceof Double d) {
            return d;
        }
        if (rawValue instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(rawValue.toString());
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
        Object rawValue = value.getValue();
        if (rawValue instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(rawValue.toString());
    }

    /**
     * Maps a Value containing a list to a List of the specified type.
     *
     * @param value         the LadybugDB Value containing a list
     * @param elementMapper function to convert each element's string value to the
     *                      target type
     * @param <T>           the type of elements in the resulting list
     * @return the list of mapped elements, or empty list if null
     */
    public static <T> List<T> asList(Value value, Function<String, T> elementMapper) {
        if (value == null || value.isNull()) {
            return List.of();
        }

        try (LbugList lbugList = new LbugList(value)) {
            long size = lbugList.getListSize();
            List<T> result = new ArrayList<>((int) size);

            for (long i = 0; i < size; i++) {
                Value element = lbugList.getListElement(i);
                result.add(elementMapper.apply(element.getValue().toString()));
            }

            return result;
        }
    }

    public static List<String> asStringList(Value value) {
        return asList(value, Function.identity());
    }

    public static List<Integer> asIntegerList(Value value) {
        return asList(value, Integer::parseInt);
    }

    public static List<Long> asLongList(Value value) {
        return asList(value, Long::parseLong);
    }

    public static List<Double> asDoubleList(Value value) {
        return asList(value, Double::parseDouble);
    }
}
