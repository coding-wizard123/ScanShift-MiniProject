package com.scanshift.service;

import com.scanshift.dao.ProductDAO;
import com.scanshift.dao.SaleDAO;
import com.scanshift.model.Product;
import com.scanshift.model.Sale;
import com.scanshift.model.SaleItem;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    public DashboardSummary getDashboardSummary() throws SQLException {
        LocalDate today = LocalDate.now();
        DashboardSummary summary = new DashboardSummary();
        summary.setTodayRevenue(saleDAO.getTotalRevenueForDate(today));
        summary.setCompletedBills(saleDAO.getCompletedBillsForDate(today));
        summary.setItemsSold(saleDAO.getQuantitySoldForDate(today));
        summary.setProductCount(productDAO.getProductCount());
        summary.setLowStockCount(productDAO.getLowStockCount());
        summary.setInventoryUnits(productDAO.getTotalInventoryUnits());
        summary.setInventoryValue(productDAO.getInventoryWholesaleValue());
        return summary;
    }

    public DailyReport getDailyReport(LocalDate date) throws SQLException {
        DailyReport report = new DailyReport();
        report.setReportDate(date);
        report.setCompletedBills(saleDAO.getCompletedBillsForDate(date));
        report.setQuantitySold(saleDAO.getQuantitySoldForDate(date));
        report.setWholesaleTotal(saleDAO.getWholesaleSalesForDate(date));
        report.setRetailTotal(saleDAO.getRetailSalesForDate(date));
        report.setGrandTotal(saleDAO.getTotalRevenueForDate(date));
        report.setSaleItems(saleDAO.findDetailedSalesForDate(date));
        return report;
    }

    public List<Product> getProducts() throws SQLException {
        return productDAO.findAll();
    }

    public List<Sale> getSalesForDate(LocalDate date) throws SQLException {
        return saleDAO.findSalesForDate(date);
    }

    public List<SaleItem> getSalesHistoryForDateRange(LocalDate startDate, LocalDate endDate,
                                                     String barcode, String productName, String scannerType) throws SQLException {
        List<Sale> sales = saleDAO.findSalesByFilter(startDate, endDate, barcode, productName, scannerType);
        List<SaleItem> allItems = new java.util.ArrayList<>();
        for (Sale sale : sales) {
            allItems.addAll(saleDAO.findItemsForSale(sale.getId()));
        }
        return allItems;
    }

    public static class DashboardSummary {
        private BigDecimal todayRevenue = BigDecimal.ZERO;
        private int completedBills;
        private int itemsSold;
        private int productCount;
        private int lowStockCount;
        private int inventoryUnits;
        private BigDecimal inventoryValue = BigDecimal.ZERO;

        public BigDecimal getTodayRevenue() {
            return todayRevenue;
        }

        public void setTodayRevenue(BigDecimal todayRevenue) {
            this.todayRevenue = todayRevenue == null ? BigDecimal.ZERO : todayRevenue;
        }

        public int getCompletedBills() {
            return completedBills;
        }

        public void setCompletedBills(int completedBills) {
            this.completedBills = completedBills;
        }

        public int getItemsSold() {
            return itemsSold;
        }

        public void setItemsSold(int itemsSold) {
            this.itemsSold = itemsSold;
        }

        public int getProductCount() {
            return productCount;
        }

        public void setProductCount(int productCount) {
            this.productCount = productCount;
        }

        public int getLowStockCount() {
            return lowStockCount;
        }

        public void setLowStockCount(int lowStockCount) {
            this.lowStockCount = lowStockCount;
        }

        public int getInventoryUnits() {
            return inventoryUnits;
        }

        public void setInventoryUnits(int inventoryUnits) {
            this.inventoryUnits = inventoryUnits;
        }

        public BigDecimal getInventoryValue() {
            return inventoryValue;
        }

        public void setInventoryValue(BigDecimal inventoryValue) {
            this.inventoryValue = inventoryValue == null ? BigDecimal.ZERO : inventoryValue;
        }
    }

    public static class DailyReport {
        private LocalDate reportDate;
        private int completedBills;
        private int quantitySold;
        private BigDecimal wholesaleTotal = BigDecimal.ZERO;
        private BigDecimal retailTotal = BigDecimal.ZERO;
        private BigDecimal grandTotal = BigDecimal.ZERO;
        private List<SaleItem> saleItems;

        public LocalDate getReportDate() {
            return reportDate;
        }

        public void setReportDate(LocalDate reportDate) {
            this.reportDate = reportDate;
        }

        public int getCompletedBills() {
            return completedBills;
        }

        public void setCompletedBills(int completedBills) {
            this.completedBills = completedBills;
        }

        public int getQuantitySold() {
            return quantitySold;
        }

        public void setQuantitySold(int quantitySold) {
            this.quantitySold = quantitySold;
        }

        public BigDecimal getWholesaleTotal() {
            return wholesaleTotal;
        }

        public void setWholesaleTotal(BigDecimal wholesaleTotal) {
            this.wholesaleTotal = wholesaleTotal == null ? BigDecimal.ZERO : wholesaleTotal;
        }

        public BigDecimal getRetailTotal() {
            return retailTotal;
        }

        public void setRetailTotal(BigDecimal retailTotal) {
            this.retailTotal = retailTotal == null ? BigDecimal.ZERO : retailTotal;
        }

        public BigDecimal getGrandTotal() {
            return grandTotal;
        }

        public void setGrandTotal(BigDecimal grandTotal) {
            this.grandTotal = grandTotal == null ? BigDecimal.ZERO : grandTotal;
        }

        public List<SaleItem> getSaleItems() {
            return saleItems;
        }

        public void setSaleItems(List<SaleItem> saleItems) {
            this.saleItems = saleItems;
        }
    }
}
