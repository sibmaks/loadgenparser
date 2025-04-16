package com.github.sibmaks.service;

import com.github.sibmaks.RequestStats;
import com.github.sibmaks.dto.RequestKey;
import com.github.sibmaks.dto.RequestKind;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ExcelWriter {

    private static void addRequestStatisticsSheet(
            Map<RequestKey, RequestStats> stats,
            XSSFWorkbook workbook
    ) {
        addSheet(stats, workbook, "Request Statistics", RequestKind.ALL);
        addSheet(stats, workbook, "Request Statistics Static", RequestKind.STATIC);
        addSheet(stats, workbook, "Request Statistics Dynamic", RequestKind.DYNAMIC);
    }

    private static void addSheet(Map<RequestKey, RequestStats> stats,
                                 XSSFWorkbook workbook,
                                 String title,
                                 RequestKind requestKind) {
        var sheet = workbook.createSheet(title);

        var headerStyle = createHeaderStyle(workbook);

        var headers = createHeaders(sheet, headerStyle);

        var rowNum = 1;
        for (var entry : stats.entrySet()) {
            var key = entry.getKey();
            if(key.requestKind() != requestKind) {
                continue;
            }
            var row = sheet.createRow(rowNum++);
            var stat = entry.getValue();

            addRow(row, key, stat);
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void addRPSSheet(
            Map<Long, Integer> rpsStats,
            XSSFWorkbook workbook
    ) {
        var sheet = workbook.createSheet("RPS Report");
        var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault());

        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Minute");
        headerRow.createCell(1).setCellValue("Requests");

        int rowNum = 1;
        for (var entry : rpsStats.entrySet()) {
            var row = sheet.createRow(rowNum++);
            var instant = Instant.ofEpochSecond(entry.getKey());
            var minute = formatter.format(instant);
            row.createCell(0).setCellValue(minute);
            row.createCell(1).setCellValue(entry.getValue());
        }
    }

    private static void addRow(XSSFRow row, RequestKey key, RequestStats stat) {
        row.createCell(0, CellType.STRING).setCellValue(key.kind());
        row.createCell(1, CellType.NUMERIC).setCellValue(stat.getCount());
        row.createCell(2, CellType.NUMERIC).setCellValue(stat.getTotalTime().doubleValue());
        row.createCell(3, CellType.NUMERIC).setCellValue(stat.getAverageTime().doubleValue());
        row.createCell(4, CellType.NUMERIC).setCellValue(stat.getVariance().doubleValue());
        row.createCell(5, CellType.NUMERIC).setCellValue(stat.getPercentile90().doubleValue());
        row.createCell(6, CellType.NUMERIC).setCellValue(stat.getPercentile95().doubleValue());
        row.createCell(7, CellType.NUMERIC).setCellValue(stat.getPercentile99().doubleValue());
        row.createCell(8, CellType.NUMERIC).setCellValue(stat.getMin().doubleValue());
        row.createCell(9, CellType.NUMERIC).setCellValue(stat.getMax().doubleValue());
        row.createCell(10, CellType.NUMERIC).setCellValue(stat.getRPS());
    }

    private static String[] createHeaders(XSSFSheet sheet, CellStyle headerStyle) {
        var headerRow = sheet.createRow(0);
        var headers = new String[]{
                "Kind",
                "Total Requests",
                "Total Time (ms)",
                "Avg Time (ms)",
                "Variance (ms)",
                "90% (ms)",
                "95% (ms)",
                "99% (ms)",
                "Min (ms)",
                "Max (ms)",
                "RPS"
        };
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

    public void write(
            Map<RequestKey, RequestStats> stats,
            Map<Long, Integer> rpsStats,
            String filename
    ) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            addRequestStatisticsSheet(stats, workbook);
            addRPSSheet(rpsStats, workbook);

            try (var fileOut = new FileOutputStream(filename)) {
                workbook.write(fileOut);
            }
        }
    }
}
