USE scanshift_db;

INSERT INTO products (barcode, product_name, wholesale_price, retail_price, stock, low_stock_threshold)
SELECT * FROM (
    SELECT '1001' AS barcode, 'Rice 5kg' AS product_name, 450.00 AS wholesale_price, 500.00 AS retail_price, 25 AS stock, 10 AS low_stock_threshold
    UNION ALL
    SELECT '1002', 'Sugar 1kg', 40.00, 48.00, 45, 10
    UNION ALL
    SELECT '1003', 'Oil 1L', 110.00, 125.00, 18, 10
) AS demo
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE barcode = demo.barcode
);

SELECT 'Demo products added if missing.' AS status;
