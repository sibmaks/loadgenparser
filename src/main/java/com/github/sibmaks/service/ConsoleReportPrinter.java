package com.github.sibmaks.service;

import com.github.sibmaks.RequestStats;
import com.github.sibmaks.dto.RequestKey;
import com.github.sibmaks.dto.RequestKind;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ConsoleReportPrinter {

    private static final String[] HEADERS = {
            "Kind", "Total", "Total Time", "Avg Time", "Variance",
            "P90", "P95", "P99", "Min", "Max", "RPS"
    };

    public void printRequestStats(Map<RequestKey, RequestStats> stats) {
        printHeader("REQUEST STATISTICS: ALL");
        stats.entrySet().stream()
                .filter(e -> e.getKey().requestKind() == RequestKind.ALL)
                .forEach(e -> printRow(e.getKey(), e.getValue()));

        printHeader("REQUEST STATISTICS: STATIC");
        stats.entrySet().stream()
                .filter(e -> e.getKey().requestKind() == RequestKind.STATIC)
                .forEach(e -> printRow(e.getKey(), e.getValue()));

        printHeader("REQUEST STATISTICS: DYNAMIC");
        stats.entrySet().stream()
                .filter(e -> e.getKey().requestKind() == RequestKind.DYNAMIC)
                .forEach(e -> printRow(e.getKey(), e.getValue()));
    }

    private void printHeader(String title) {
        System.out.println("\n\u001B[1;34m" + title + "\u001B[0m");
        System.out.printf("%-15s %-15s %-20s %-20s %-20s %-16s %-16s %-16s %-16s %-16s %-10s%n",
                (Object[]) HEADERS);
        System.out.println("-----------------------------------------------------------------------------------------------");
    }

    private void printRow(RequestKey key, RequestStats stat) {
        System.out.printf("%-15s %-15d %-20.2f %-20.2f %-20.2f %-16.2f %-16.2f %-16.2f %-16.2f %-16.2f %-10d%n",
                key.kind(),
                stat.getCount(),
                stat.getTotalTime().doubleValue(),
                stat.getAverageTime().doubleValue(),
                stat.getVariance().doubleValue(),
                stat.getPercentile90().doubleValue(),
                stat.getPercentile95().doubleValue(),
                stat.getPercentile99().doubleValue(),
                stat.getMin().doubleValue(),
                stat.getMax().doubleValue(),
                stat.getRPS()
        );
    }
}
