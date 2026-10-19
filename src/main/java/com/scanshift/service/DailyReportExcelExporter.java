package com.scanshift.service;

import com.scanshift.model.SaleItem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DailyReportExcelExporter {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String CURRENCY_FORMAT = "\"₹\"#,##0.00;[Red](\"₹\"#,##0.00)";

    private DailyReportExcelExporter() {
    }

    public static void export(ReportService.DailyReport report, Path destination) throws IOException {
        if (report == null || destination == null) {
            throw new IllegalArgumentException("A report and destination file are required.");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Sales Report");
            createTitle(sheet, workbook, report);
            createSummary(sheet, workbook, report);
            createSalesRecords(sheet, workbook, report.getSaleItems());
            setColumnWidths(sheet);

            try (OutputStream output = Files.newOutputStream(destination)) {
                workbook.write(output);
            }
        }
    }

    private static void createTitle(Sheet sheet, Workbook workbook, ReportService.DailyReport report) {
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ScanShift Daily Sales Report");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 7));

        Row dateRow = sheet.createRow(1);
        dateRow.createCell(0).setCellValue("Report date");
        dateRow.createCell(1).setCellValue(report.getReportDate().toString());
    }

    private static void createSummary(Sheet sheet, Workbook workbook, ReportService.DailyReport report) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        Row heading = sheet.createRow(3);
        Cell metricHeading = heading.createCell(0);
        metricHeading.setCellValue("Daily Summary");
        metricHeading.setCellStyle(headerStyle);
        Cell valueHeading = heading.createCell(1);
        valueHeading.setCellValue("Value");
        valueHeading.setCellStyle(headerStyle);

        addSummaryRow(sheet, 4, "Total sales", report.getGrandTotal(), currencyStyle);
        addSummaryRow(sheet, 5, "Number of transactions", report.getCompletedBills(), null);
        addSummaryRow(sheet, 6, "Wholesale sales", report.getWholesaleTotal(), currencyStyle);
        addSummaryRow(sheet, 7, "Retail sales", report.getRetailTotal(), currencyStyle);
        addSummaryRow(sheet, 8, "Quantity sold", report.getQuantitySold(), null);
    }

    private static void addSummaryRow(Sheet sheet, int rowIndex, String label, BigDecimal value,
                                      CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value == null ? 0 : value.doubleValue());
        if (style != null) {
            valueCell.setCellStyle(style);
        }
    }

    private static void addSummaryRow(Sheet sheet, int rowIndex, String label, int value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        if (style != null) {
            valueCell.setCellStyle(style);
        }
    }

    private static void createSalesRecords(Sheet sheet, Workbook workbook, List<SaleItem> items) {
        int headingRowIndex = 11;
        Row heading = sheet.createRow(headingRowIndex);
        String[] columns = {"Bill #", "Time", "Barcode", "Product", "Scanner", "Unit Price", "Quantity", "Line Total"};
        CellStyle headerStyle = createHeaderStyle(workbook);
        for (int column = 0; column < columns.length; column++) {
            Cell cell = heading.createCell(column);
            cell.setCellValue(columns[column]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle currencyStyle = createCurrencyStyle(workbook);
        int rowIndex = headingRowIndex + 1;
        if (items == null || items.isEmpty()) {
            Row emptyRow = sheet.createRow(rowIndex);
            emptyRow.createCell(0).setCellValue("No sales records for this date.");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIndex, rowIndex, 0, 7));
            return;
        }

        for (SaleItem item : items) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(item.getSaleId() == null ? 0 : item.getSaleId());
            row.createCell(1).setCellValue(item.getSaleDate() == null ? "" : item.getSaleDate().format(TIME_FORMATTER));
            row.createCell(2).setCellValue(item.getBarcode() == null ? "" : item.getBarcode());
            row.createCell(3).setCellValue(item.getProductName() == null ? "" : item.getProductName());
            row.createCell(4).setCellValue(item.getScannerType() == null ? "" : item.getScannerType());
            setCurrencyCell(row.createCell(5), item.getUnitPrice(), currencyStyle);
            row.createCell(6).setCellValue(item.getQuantity());
            setCurrencyCell(row.createCell(7), item.getLineTotal(), currencyStyle);
        }
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                headingRowIndex, rowIndex - 1, 0, columns.length - 1));
        sheet.createFreezePane(0, headingRowIndex + 1);
    }

    private static void setCurrencyCell(Cell cell, BigDecimal amount, CellStyle style) {
        cell.setCellValue(amount == null ? 0 : amount.doubleValue());
        cell.setCellStyle(style);
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static CellStyle createCurrencyStyle(Workbook workbook) {
        DataFormat format = workbook.createDataFormat();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(format.getFormat(CURRENCY_FORMAT));
        return style;
    }

    private static void setColumnWidths(Sheet sheet) {
        int[] widths = {14, 12, 20, 32, 16, 18, 12, 18};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, widths[column] * 256);
        }
    }
}
