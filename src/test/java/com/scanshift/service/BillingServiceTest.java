package com.scanshift.service;

import com.scanshift.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BillingServiceTest {

    @Test
    void wholesalePriceShouldBeUsedForWholesaleMode() {
        Product product = new Product();
        product.setWholesalePrice(new BigDecimal("450.00"));
        product.setRetailPrice(new BigDecimal("500.00"));

        assertEquals(new BigDecimal("450.00"), BillingService.getUnitPrice(product, "WHOLESALE"));
        assertEquals(new BigDecimal("500.00"), BillingService.getUnitPrice(product, "RETAIL"));
    }

    @Test
    void lineTotalShouldMultiplyPriceAndQuantity() {
        BigDecimal lineTotal = BillingService.calculateLineTotal(new BigDecimal("125.00"), 3);
        assertEquals(new BigDecimal("375.00"), lineTotal);
    }

    @Test
    void shouldRejectInvalidQuantity() {
        assertThrows(IllegalArgumentException.class, () -> BillingService.calculateLineTotal(new BigDecimal("50.00"), 0));
    }

    @Test
    void shouldRespectStockAvailability() {
        Product product = new Product();
        product.setStock(4);
        assertTrue(BillingService.hasSufficientStock(product, 4));
        assertFalse(BillingService.hasSufficientStock(product, 5));
    }

    @Test
    void shouldAccountForExistingCartQuantityWhenAddingRepeatedScans() {
        Product product = new Product();
        product.setStock(5);

        assertTrue(BillingService.canAddQuantity(product, 3, 2));
        assertFalse(BillingService.canAddQuantity(product, 3, 3));
    }

    @Test
    void scannerTypeShouldBeNormalized() {
        assertEquals("WHOLESALE", BillingService.sanitizeScannerType("wholesale"));
        assertEquals("RETAIL", BillingService.sanitizeScannerType("retail"));
    }
}
