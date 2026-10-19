package com.scanshift.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class PriceFormatter {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private PriceFormatter() {
    }

    public static String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "₹0.00";
        }
        return CURRENCY_FORMAT.format(value);
    }

    public static String formatPlain(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
