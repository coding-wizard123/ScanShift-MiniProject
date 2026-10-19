package com.scanshift.service;

import com.scanshift.dao.ProductDAO;
import com.scanshift.model.Product;

import java.sql.SQLException;
import java.util.Optional;

public class InventoryService {
    private final ProductDAO productDAO = new ProductDAO();
    public Optional<Product> findProductByBarcode(String barcode) throws SQLException {
        return productDAO.findByBarcode(barcode);
    }

    public void addProduct(Product product) throws SQLException {
        productDAO.save(product);
    }

    public void updateProduct(Product product) throws SQLException {
        productDAO.update(product);
    }

    public void restockProduct(Long productId, int quantity, String reason) throws SQLException {
        Product product = productDAO.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        int previousStock = product.getStock();
        int newStock = previousStock + quantity;
        product.setStock(newStock);
        productDAO.update(product);

    }

    public void adjustInventory(Long productId, int quantity, String reason) throws SQLException {
        Product product = productDAO.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found."));
        int previousStock = product.getStock();
        int newStock = previousStock + quantity;
        if (newStock < 0) {
            throw new IllegalArgumentException("Inventory adjustment cannot result in negative stock.");
        }

        product.setStock(newStock);
        productDAO.update(product);

    }

    public boolean isLowStock(Product product) {
        return product != null && product.getStock() <= product.getLowStockThreshold();
    }

    public boolean isOutOfStock(Product product) {
        return product != null && product.getStock() <= 0;
    }
}
