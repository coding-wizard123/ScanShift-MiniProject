# ScanShift

ScanShift is a Java Swing desktop billing and inventory application for retail shops. It uses JDBC and MySQL for product, stock, transaction, and reporting data.

## Features

- Dashboard metrics and recent sales loaded from MySQL
- Dual-scanner billing with wholesale and retail pricing
- Product creation, editing, searching, and barcode validation
- Inventory stock adjustments and restocking
- Transaction-safe checkout with stock deduction
- Daily sales reports, sales history, printing, and Excel (`.xlsx`) export
- JUnit tests for billing rules and Excel report generation

## Technologies

- Java 21
- Swing
- Maven
- MySQL Connector/J and JDBC
- Apache POI (OOXML) for `.xlsx` reports
- JUnit 5

## Prerequisites

- JDK 21
- Maven 3.9 or later
- MySQL Server
- An existing MySQL database configured with the schema expected by the application

## Database setup

The application connects to the `scanshift` database by default. It does not create or reset databases, tables, or product data. Configure the database with these tables and columns:

- `products`: `id`, `barcode`, `product_name`, `wholesale_price`, `retail_price`, `stock`, `low_stock_limit`
- `sales`: `id`, `sale_date`, `wholesale_total`, `retail_total`, `grand_total`
- `sale_items`: `id`, `sale_id`, `product_id`, `barcode`, `product_name`, `scanner_type`, `price`, `quantity`, `total`

The existing SQL files in [`database/`](./database/) are legacy/demo scripts for a different `scanshift_db` schema. They do **not** match the schema used by the current application; do not run them to initialize the database used by ScanShift.

## Database configuration

The application reads non-secret connection settings from `db.properties` in the project root or from the classpath resource. The tracked [`src/main/resources/db.properties.example`](./src/main/resources/db.properties.example) shows the available settings. Copy it to `db.properties` and adjust the host, port, database, and username as needed. Local `db.properties` files are ignored by Git.

The MySQL password is read only from the `SCANSHIFT_DB_PASSWORD` environment variable. Do not put passwords in `db.properties`, `.env` files, source code, or commits.

In PowerShell, enter the password securely and set it for the current session:

```powershell
$secure = Read-Host "MySQL password" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:SCANSHIFT_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
}
```

Clear it when finished:

```powershell
$env:SCANSHIFT_DB_PASSWORD = $null
```

## Build, test, and run

Run these commands from the project root in a PowerShell session with JDK 21, Maven, and the password configured:

```powershell
mvn clean test
mvn clean package
mvn exec:java
```

To build with the Maven distribution bundled in a local development copy of the project, if available:

```powershell
& ".\tools\apache-maven-3.9.9\bin\mvn.cmd" clean test
& ".\tools\apache-maven-3.9.9\bin\mvn.cmd" clean package
& ".\tools\apache-maven-3.9.9\bin\mvn.cmd" exec:java
```

Before starting, ensure MySQL is running and the configured database and compatible tables already exist. The bundled Maven distribution is excluded from source control; install Maven separately or use Maven from your `PATH`.

## Billing and report verification

Scan or enter a barcode that exists in `products`. Confirm Scanner 1 uses `wholesale_price` and Scanner 2 uses `retail_price`. On checkout, ScanShift records the sale and its sale items and deducts stock within a database transaction. Daily reports read actual MySQL records; the Excel export prompts for a destination and writes the selected date's summary and sale details to an `.xlsx` workbook.
