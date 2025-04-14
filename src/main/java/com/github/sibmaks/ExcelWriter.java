package com.github.sibmaks;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class ExcelWriter {
    public static void writeExcelReport(Map<RequestKey, RequestStats> stats, String filename) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Request Statistics");
            var staticSheet = workbook.createSheet("Static Request");
            var dynamicSheet = workbook.createSheet("Dynamic Request");

            var headerStyle = createHeaderStyle(workbook);

            var headers = createHeaders(sheet, headerStyle);
            createHeaders(staticSheet, headerStyle);
            createHeaders(dynamicSheet, headerStyle);

            var rowNum = 1;
            var staticRowNum = 1;
            var dynamicRowNum = 1;
            for (var entry : stats.entrySet()) {
                var row = sheet.createRow(rowNum++);
                var key = entry.getKey();
                var stat = entry.getValue();

                addRow(row, key, stat);

                if(key.requestKind() == RequestKind.STATIC) {
                    var staticRow = staticSheet.createRow(staticRowNum++);
                    addRow(staticRow, key, stat);
                } else if (key.requestKind() == RequestKind.DYNAMIC) {
                    var dynamicRow = dynamicSheet.createRow(dynamicRowNum++);
                    addRow(dynamicRow, key, stat);
                }
            }

            for (var i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                staticSheet.autoSizeColumn(i);
                dynamicSheet.autoSizeColumn(i);
            }

            try (var fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
            }
        }
    }

    private static void addRow(XSSFRow row, RequestKey key, RequestStats stat) {
        row.createCell(0, CellType.STRING).setCellValue(key.method());
        row.createCell(1, CellType.STRING).setCellValue(key.uri());
        row.createCell(2, CellType.NUMERIC).setCellValue(stat.getCount());
        row.createCell(3, CellType.NUMERIC).setCellValue(stat.getTotalTime().doubleValue());
        row.createCell(4, CellType.NUMERIC).setCellValue(stat.getAverageTime().doubleValue());
        row.createCell(5, CellType.NUMERIC).setCellValue(stat.getVariance().doubleValue());
        row.createCell(6, CellType.NUMERIC).setCellValue(stat.getPercentile99().doubleValue());
        row.createCell(7, CellType.NUMERIC).setCellValue(stat.getMin().doubleValue());
        row.createCell(8, CellType.NUMERIC).setCellValue(stat.getMax().doubleValue());
    }

    private static String[] createHeaders(XSSFSheet sheet, CellStyle headerStyle) {
        var headerRow = sheet.createRow(0);
        var headers = new String[]{"HTTP Method", "URI", "Total Requests", "Total Time (ms)", "Avg Time (ms)", "Variance (ms)", "99% (ms)", "Min (ms)", "Max (ms)"};
        for (var i = 0; i < headers.length; i++) {
            var cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return headers;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        var style = workbook.createCellStyle();
        var font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
