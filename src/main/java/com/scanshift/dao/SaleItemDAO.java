package com.scanshift.dao;

import com.scanshift.config.DatabaseConnection;
import com.scanshift.model.SaleItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SaleItemDAO {

    public void saveAll(List<SaleItem> items, Long saleId) throws SQLException {
        String sql = "INSERT INTO sale_items (sale_id, product_id, barcode, product_name, scanner_type, price, quantity, total) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SaleItem item : items) {
                statement.setLong(1, saleId);
                if (item.getProductId() != null) {
                    statement.setLong(2, item.getProductId());
                } else {
                    statement.setNull(2, java.sql.Types.BIGINT);
                }
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
    }
}
