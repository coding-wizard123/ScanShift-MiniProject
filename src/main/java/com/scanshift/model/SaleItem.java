package com.scanshift.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleItem {
    private Long id;
    private Long saleId;
    private Long productId;
    private String barcode;
    private String productName;
    private String scannerType;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private LocalDateTime saleDate;
    private LocalDateTime createdAt;

    public SaleItem() {
    }

    public SaleItem(Long id, Long saleId, Long productId, String barcode, String productName,
                   String scannerType, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                   LocalDateTime createdAt) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.barcode = barcode;
        this.productName = productName;
        this.scannerType = scannerType;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSaleId() {
        return saleId;
    }

    public void setSaleId(Long saleId) {
        this.saleId = saleId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getScannerType() {
        return scannerType;
    }

    public void setScannerType(String scannerType) {
        this.scannerType = scannerType;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
