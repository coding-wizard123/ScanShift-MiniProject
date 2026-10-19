package com.scanshift.service;

import com.scanshift.model.SaleItem;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyReportExcelExporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsDatabaseReportValuesAndSalesRecordsAsFormattedWorkbook() throws Exception {
        ReportService.DailyReport report = new ReportService.DailyReport();
        report.setReportDate(LocalDate.of(2026, 10, 19));
        report.setGrandTotal(new BigDecimal("125.50"));
        report.setCompletedBills(1);
        report.setWholesaleTotal(new BigDecimal("50.00"));
        report.setRetailTotal(new BigDecimal("75.50"));
        report.setQuantitySold(3);

        SaleItem item = new SaleItem();
        item.setSaleId(42L);
        item.setSaleDate(LocalDateTime.of(2026, 10, 19, 13, 15, 0));
        item.setBarcode("890123");
        item.setProductName("Coffee");
        item.setScannerType("RETAIL");
        item.setUnitPrice(new BigDecimal("25.50"));
        item.setQuantity(3);
        item.setLineTotal(new BigDecimal("76.50"));
        report.setSaleItems(List.of(item));

        Path file = temporaryDirectory.resolve("daily-report.xlsx");
        DailyReportExcelExporter.export(report, file);

        assertTrue(Files.size(file) > 0);
        try (var workbook = WorkbookFactory.create(file.toFile())) {
            var sheet = workbook.getSheet("Daily Sales Report");
            assertEquals("ScanShift Daily Sales Report", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("2026-10-19", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals(125.50, sheet.getRow(4).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(1, sheet.getRow(5).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(50.00, sheet.getRow(6).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(75.50, sheet.getRow(7).getCell(1).getNumericCellValue(), 0.001);
            assertEquals("Coffee", sheet.getRow(12).getCell(3).getStringCellValue());
            assertEquals(76.50, sheet.getRow(12).getCell(7).getNumericCellValue(), 0.001);
            assertTrue(sheet.getRow(12).getCell(7).getCellStyle().getDataFormatString().contains("₹"));
            assertTrue(sheet.getColumnWidth(3) > sheet.getColumnWidth(0));
        }
    }

    @Test
    void exportsAnExplicitEmptyRecordsSectionForEmptyReport() throws Exception {
        ReportService.DailyReport report = new ReportService.DailyReport();
        report.setReportDate(LocalDate.of(2026, 10, 19));
        report.setSaleItems(List.of());
        Path file = temporaryDirectory.resolve("empty-report.xlsx");

        DailyReportExcelExporter.export(report, file);

        try (var workbook = WorkbookFactory.create(file.toFile())) {
            assertEquals("No sales records for this date.",
                    workbook.getSheet("Daily Sales Report").getRow(12).getCell(0).getStringCellValue());
        }
    }
}
