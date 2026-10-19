package com.scanshift.dao;

import com.scanshift.config.DatabaseConnection;
import com.scanshift.model.Sale;
import com.scanshift.model.SaleItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaleDAO {

    public Sale save(Sale sale) throws SQLException {
        String sql = "INSERT INTO sales (sale_date, wholesale_total, retail_total, grand_total) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(sale.getSaleDate()));
            statement.setBigDecimal(2, sale.getWholesaleTotal());
            statement.setBigDecimal(3, sale.getRetailTotal());
            statement.setBigDecimal(4, sale.getGrandTotal());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    sale.setId(generatedKeys.getLong(1));
                }
            }
        }
        return sale;
    }

    public Sale completeSale(Sale sale, List<SaleItem> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A sale must contain at least one item.");
        }

        BigDecimal wholesaleTotal = BigDecimal.ZERO;
        BigDecimal retailTotal = BigDecimal.ZERO;
        Map<Long, Integer> quantitiesByProduct = new LinkedHashMap<>();
        for (SaleItem item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() <= 0
                    || item.getUnitPrice() == null || item.getUnitPrice().signum() <= 0
                    || item.getBarcode() == null || item.getBarcode().isBlank()
                    || item.getProductName() == null || item.getProductName().isBlank()) {
                throw new IllegalArgumentException("Sale items require a product, barcode, name, positive quantity, and positive price.");
            }
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            item.setLineTotal(lineTotal);
            String scannerType = item.getScannerType();
            if ("WHOLESALE".equals(scannerType)) {
                wholesaleTotal = wholesaleTotal.add(lineTotal);
            } else if ("RETAIL".equals(scannerType)) {
                retailTotal = retailTotal.add(lineTotal);
            } else {
                throw new IllegalArgumentException("Scanner type must be WHOLESALE or RETAIL.");
            }
            quantitiesByProduct.merge(item.getProductId(), item.getQuantity(), Math::addExact);
        }

        sale.setWholesaleTotal(wholesaleTotal);
        sale.setRetailTotal(retailTotal);
        sale.setGrandTotal(wholesaleTotal.add(retailTotal));
        if (sale.getSaleDate() == null) {
            sale.setSaleDate(LocalDateTime.now());
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String saleSql = "INSERT INTO sales (sale_date, wholesale_total, retail_total, grand_total) VALUES (?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setTimestamp(1, Timestamp.valueOf(sale.getSaleDate()));
                    statement.setBigDecimal(2, wholesaleTotal);
                    statement.setBigDecimal(3, retailTotal);
                    statement.setBigDecimal(4, sale.getGrandTotal());
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new SQLException("MySQL did not return an ID for the saved sale.");
                        }
                        sale.setId(generatedKeys.getLong(1));
                    }
                }

                String itemSql = "INSERT INTO sale_items "
                        + "(sale_id, product_id, barcode, product_name, scanner_type, price, quantity, total) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                    for (SaleItem item : items) {
                        statement.setLong(1, sale.getId());
                        statement.setLong(2, item.getProductId());
                        statement.setString(3, item.getBarcode());
                        statement.setString(4, item.getProductName());
                        statement.setString(5, item.getScannerType());
                        statement.setBigDecimal(6, item.getUnitPrice());
                        statement.setInt(7, item.getQuantity());
                        statement.setBigDecimal(8, item.getLineTotal());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                String stockSql = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";
                try (PreparedStatement statement = connection.prepareStatement(stockSql)) {
                    for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {
                        statement.setInt(1, entry.getValue());
                        statement.setLong(2, entry.getKey());
                        statement.setInt(3, entry.getValue());
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Insufficient stock or product no longer exists (product ID "
                                    + entry.getKey() + "). Sale was not completed.");
                        }
                    }
                }

                connection.commit();
                return sale;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public List<Sale> findSalesForDate(LocalDate date) throws SQLException {
        String sql = "SELECT * FROM sales WHERE DATE(sale_date) = ? ORDER BY sale_date DESC";
        List<Sale> sales = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sales.add(mapSale(resultSet));
                }
            }
        }
        return sales;
    }

    public List<Sale> findSalesByFilter(LocalDate startDate, LocalDate endDate, String barcode, String productName,
                                      String scannerType) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT s.* FROM sales s " +
                        "LEFT JOIN sale_items si ON si.sale_id = s.id WHERE 1 = 1"
        );
        List<Object> parameters = new ArrayList<>();

        if (startDate != null) {
            sql.append(" AND DATE(s.sale_date) >= ?");
            parameters.add(startDate.toString());
        }
        if (endDate != null) {
            sql.append(" AND DATE(s.sale_date) <= ?");
            parameters.add(endDate.toString());
        }
        if (barcode != null && !barcode.isBlank()) {
            sql.append(" AND si.barcode LIKE ?");
            parameters.add("%" + barcode.trim() + "%");
        }
        if (productName != null && !productName.isBlank()) {
            sql.append(" AND si.product_name LIKE ?");
            parameters.add("%" + productName.trim() + "%");
        }
        if (scannerType != null && !scannerType.isBlank()) {
            sql.append(" AND si.scanner_type = ?");
            parameters.add(scannerType.trim());
        }

        sql.append(" ORDER BY s.sale_date DESC");

        List<Sale> sales = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sales.add(mapSale(resultSet));
                }
            }
        }
        return sales;
    }

    public List<SaleItem> findItemsForSale(Long saleId) throws SQLException {
        String sql = "SELECT si.id, si.sale_id, si.product_id, si.barcode, si.product_name, si.scanner_type, "
                + "si.price AS unit_price, si.quantity, si.total AS line_total, s.sale_date "
                + "FROM sale_items si JOIN sales s ON s.id = si.sale_id WHERE si.sale_id = ? ORDER BY si.id ASC";
        List<SaleItem> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapSaleItem(resultSet));
                }
            }
        }
        return items;
    }

    public Sale getSaleById(Long saleId) throws SQLException {
        String sql = "SELECT * FROM sales WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSale(resultSet);
                }
            }
        }
        return null;
    }

    public BigDecimal getTotalRevenueForDate(LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(grand_total), 0) FROM sales WHERE DATE(sale_date) = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public int getCompletedBillsForDate(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    public int getQuantitySoldForDate(LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(si.quantity), 0) FROM sale_items si "
                + "JOIN sales s ON s.id = si.sale_id WHERE DATE(s.sale_date) = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    public BigDecimal getWholesaleSalesForDate(LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(wholesale_total), 0) FROM sales WHERE DATE(sale_date) = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getRetailSalesForDate(LocalDate date) throws SQLException {
        String sql = "SELECT COALESCE(SUM(retail_total), 0) FROM sales WHERE DATE(sale_date) = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    public List<SaleItem> findDetailedSalesForDate(LocalDate date) throws SQLException {
        String sql = "SELECT si.id, si.sale_id, si.product_id, si.barcode, si.product_name, si.scanner_type, "
                + "si.price AS unit_price, si.quantity, si.total AS line_total, s.sale_date FROM sale_items si "
                + "JOIN sales s ON s.id = si.sale_id WHERE DATE(s.sale_date) = ? ORDER BY s.sale_date DESC, si.id ASC";
        List<SaleItem> items = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, date.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    items.add(mapSaleItem(resultSet));
                }
            }
        }
        return items;
    }

    private Sale mapSale(ResultSet resultSet) throws SQLException {
        Sale sale = new Sale();
        sale.setId(resultSet.getLong("id"));
        Timestamp saleDate = resultSet.getTimestamp("sale_date");
        if (saleDate != null) {
            sale.setSaleDate(saleDate.toLocalDateTime());
        }
        sale.setWholesaleTotal(resultSet.getBigDecimal("wholesale_total"));
        sale.setRetailTotal(resultSet.getBigDecimal("retail_total"));
        sale.setGrandTotal(resultSet.getBigDecimal("grand_total"));
        return sale;
    }

    private SaleItem mapSaleItem(ResultSet resultSet) throws SQLException {
        SaleItem item = new SaleItem();
        item.setId(resultSet.getLong("id"));
        item.setSaleId(resultSet.getLong("sale_id"));
        long productId = resultSet.getLong("product_id");
        item.setProductId(resultSet.wasNull() ? null : productId);
        item.setBarcode(resultSet.getString("barcode"));
        item.setProductName(resultSet.getString("product_name"));
        item.setScannerType(resultSet.getString("scanner_type"));
        item.setUnitPrice(resultSet.getBigDecimal("unit_price"));
        item.setQuantity(resultSet.getInt("quantity"));
        item.setLineTotal(resultSet.getBigDecimal("line_total"));
        Timestamp saleDate = resultSet.getTimestamp("sale_date");
        if (saleDate != null) {
            item.setSaleDate(saleDate.toLocalDateTime());
        }
        return item;
    }
}
