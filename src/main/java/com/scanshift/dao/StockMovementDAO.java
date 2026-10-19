package com.scanshift.dao;

import com.scanshift.config.DatabaseConnection;
import com.scanshift.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StockMovementDAO {

    public void save(StockMovement movement) throws SQLException {
        String sql = "INSERT INTO stock_movements (product_id, movement_type, quantity_change, previous_stock, new_stock, reason) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, movement.getProductId());
            statement.setString(2, movement.getMovementType());
            statement.setInt(3, movement.getQuantityChange());
            statement.setInt(4, movement.getPreviousStock());
            statement.setInt(5, movement.getNewStock());
            statement.setString(6, movement.getReason());
            statement.executeUpdate();
        }
    }
}
