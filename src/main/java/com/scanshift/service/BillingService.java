package com.scanshift.service;

import com.scanshift.model.Product;
import com.scanshift.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class BillingService {
    private BillingService() {
    }

    public static BigDecimal getUnitPrice(Product product, String scannerType) {
        Objects.requireNonNull(product, "product");
        if ("WHOLESALE".equalsIgnoreCase(scannerType)) {
            return product.getWholesalePrice();
        }
        if ("RETAIL".equalsIgnoreCase(scannerType)) {
            return product.getRetailPrice();
        }
        throw new IllegalArgumentException("Scanner type must be WHOLESALE or RETAIL.");
    }

    public static BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price cannot be null.");
        }
        if (!ValidationUtil.isPositiveInteger(quantity)) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean hasSufficientStock(Product product, int quantity) {
        return canAddQuantity(product, 0, quantity);
    }

    public static boolean canAddQuantity(Product product, long quantityAlreadyInCart, int additionalQuantity) {
        return product != null && quantityAlreadyInCart >= 0 && additionalQuantity > 0
                && quantityAlreadyInCart <= (long) product.getStock() - additionalQuantity;
    }

    public static String sanitizeScannerType(String scannerType) {
        if (scannerType == null) {
            return "WHOLESALE";
        }
        String normalized = scannerType.trim().toUpperCase();
        if ("WHOLESALE".equals(normalized) || "RETAIL".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Unsupported scanner type: " + scannerType);
    }
}
