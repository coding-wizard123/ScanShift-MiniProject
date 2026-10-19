package com.scanshift.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {
    private Long id;
    private LocalDateTime saleDate;
    private BigDecimal grandTotal;
    private BigDecimal wholesaleTotal;
    private BigDecimal retailTotal;
    private int totalQuantity;
    private String status;
    private LocalDateTime createdAt;

    public Sale() {
    }

    public Sale(Long id, LocalDateTime saleDate, BigDecimal grandTotal, int totalQuantity,
               String status, LocalDateTime createdAt) {
        this.id = id;
        this.saleDate = saleDate;
        this.grandTotal = grandTotal;
        this.totalQuantity = totalQuantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public BigDecimal getWholesaleTotal() {
        return wholesaleTotal;
    }

    public void setWholesaleTotal(BigDecimal wholesaleTotal) {
        this.wholesaleTotal = wholesaleTotal;
    }

    public BigDecimal getRetailTotal() {
        return retailTotal;
    }

    public void setRetailTotal(BigDecimal retailTotal) {
        this.retailTotal = retailTotal;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
