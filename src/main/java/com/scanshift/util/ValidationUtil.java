package com.scanshift.util;

import java.math.BigDecimal;
import java.util.Objects;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static String normalizeBarcode(String barcode) {
        if (barcode == null) {
            return "";
        }
        return barcode.trim();
    }

    public static boolean isValidBarcode(String barcode) {
        String normalized = normalizeBarcode(barcode);
        return !normalized.isEmpty() && normalized.length() >= 3 && normalized.length() <= 50;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNonNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) >= 0;
    }

    public static boolean isPositiveInteger(Integer value) {
        return value != null && value > 0;
    }

    public static boolean isNonNegativeInteger(Integer value) {
        return value != null && value >= 0;
    }

    public static String requireText(String value, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }
}
