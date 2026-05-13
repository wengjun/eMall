package com.emall.common.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

public final class RowMaps {
    private RowMaps() {
    }

    public static long longValue(Map<String, Object> row, String column) {
        return ((Number) value(row, column)).longValue();
    }

    public static int intValue(Map<String, Object> row, String column) {
        return ((Number) value(row, column)).intValue();
    }

    public static boolean booleanValue(Map<String, Object> row, String column) {
        Object value = value(row, column);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public static String stringValue(Map<String, Object> row, String column) {
        Object value = value(row, column);
        return value == null ? null : value.toString();
    }

    public static BigDecimal decimalValue(Map<String, Object> row, String column) {
        Object value = value(row, column);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    public static Instant instantValue(Map<String, Object> row, String column) {
        Object value = value(row, column);
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(value.toString());
    }

    public static LocalDate localDateValue(Map<String, Object> row, String column) {
        Object value = value(row, column);
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return LocalDate.parse(value.toString());
    }

    public static Object value(Map<String, Object> row, String column) {
        if (row.containsKey(column)) {
            return row.get(column);
        }
        String upperColumn = column.toUpperCase(Locale.ROOT);
        if (row.containsKey(upperColumn)) {
            return row.get(upperColumn);
        }
        return row.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(column))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
