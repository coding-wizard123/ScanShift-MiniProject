package com.scanshift.dao;

import com.scanshift.config.DatabaseConnection;
import com.scanshift.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDAO {

    public Optional<Product> findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, barcode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProduct(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findById(Long productId) throws SQLException {
        if (productId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProduct(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public Product save(Product product) throws SQLException {
        String sql = "INSERT INTO products (barcode, product_name, wholesale_price, retail_price, stock, low_stock_limit) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, product.getBarcode());
            statement.setString(2, product.getProductName());
            statement.setBigDecimal(3, product.getWholesalePrice());
            statement.setBigDecimal(4, product.getRetailPrice());
            statement.setInt(5, product.getStock());
            statement.setInt(6, product.getLowStockThreshold());
            statement.executeUpdate();

            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    product.setId(resultSet.getLong(1));
                }
            }
        }
        return product;
    }

    public void update(Product product) throws SQLException {
        String sql = "UPDATE products SET barcode = ?, product_name = ?, wholesale_price = ?, retail_price = ?, "
                + "stock = ?, low_stock_limit = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getBarcode());
            statement.setString(2, product.getProductName());
            statement.setBigDecimal(3, product.getWholesalePrice());
            statement.setBigDecimal(4, product.getRetailPrice());
            statement.setInt(5, product.getStock());
            statement.setInt(6, product.getLowStockThreshold());
            statement.setLong(7, product.getId());
            statement.executeUpdate();
        }
    }

    public List<Product> findAll() throws SQLException {
        String sql = "SELECT * FROM products ORDER BY product_name ASC";
        List<Product> products = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapProduct(resultSet));
            }
        }
        return products;
    }

    public List<Product> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM products WHERE barcode LIKE ? OR product_name LIKE ? ORDER BY product_name ASC";
        List<Product> products = new ArrayList<>();
        String filter = keyword == null ? "" : "%" + keyword.trim() + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, filter);
            statement.setString(2, filter);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(mapProduct(resultSet));
                }
            }
        }
        return products;
    }

    public List<Product> findLowStockProducts() throws SQLException {
        String sql = "SELECT * FROM products WHERE stock <= low_stock_limit ORDER BY product_name ASC";
        List<Product> products = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                products.add(mapProduct(resultSet));
            }
        }
        return products;
    }

    public int getProductCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    public int getLowStockCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE stock <= low_stock_limit";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    public int getTotalInventoryUnits() throws SQLException {
        String sql = "SELECT COALESCE(SUM(stock), 0) FROM products";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    public BigDecimal getInventoryWholesaleValue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(stock * wholesale_price), 0) FROM products";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getBigDecimal(1);
            }
        }
        return BigDecimal.ZERO;
    }

    public void updateStock(Long productId, int newStock) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, newStock);
            statement.setLong(2, productId);
            statement.executeUpdate();
        }
    }

    private Product mapProduct(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("id"));
        product.setBarcode(resultSet.getString("barcode"));
        product.setProductName(resultSet.getString("product_name"));
        product.setWholesalePrice(resultSet.getBigDecimal("wholesale_price"));
        product.setRetailPrice(resultSet.getBigDecimal("retail_price"));
        product.setStock(resultSet.getInt("stock"));
        product.setLowStockThreshold(resultSet.getInt("low_stock_limit"));
        return product;
    }
}
