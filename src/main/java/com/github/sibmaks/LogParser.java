package com.github.sibmaks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LogParser {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please provide log file path as argument");
            return;
        }

        var stats = collectStatistics(args[0]);

        printStatistics(stats);

        ExcelWriter.writeExcelReport(stats, "output.xlsx");
    }

    private static HashMap<RequestKey, RequestStats> collectStatistics(String fileName) {
        var genericStatsKey = new RequestKey("GENERIC", "all", RequestKind.GENERIC);
        var staticStatsKey = new RequestKey("STATIC", "all", RequestKind.STATIC);
        var dynamicStatsKey = new RequestKey("DYNAMIC", "all", RequestKind.DYNAMIC);

        var stats = new LinkedHashMap<RequestKey, RequestStats>();
        stats.put(genericStatsKey, new RequestStats());
        stats.put(staticStatsKey, new RequestStats());
        stats.put(dynamicStatsKey, new RequestStats());

        var logPattern = Pattern.compile(
                "^\\[(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)].*?at\\s(\\d+,\\d+)ms.*?(http://\\S+)"
        );

        try (var reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                var matcher = logPattern.matcher(line);
                if (matcher.find()) {
                    var method = matcher.group(1);
                    var time = new BigDecimal(matcher.group(2).replace(',', '.'));
                    var uri = matcher.group(3);

                    var genericRequestStats = stats.getOrDefault(genericStatsKey, new RequestStats());
                    genericRequestStats.addRequest(time);
                    stats.putIfAbsent(genericStatsKey, genericRequestStats);

                    var key = new RequestKey(method, uri, (uri.contains("/img/") || uri.contains("/cmsstatic/") || uri.contains("/js/")) ? RequestKind.STATIC : RequestKind.DYNAMIC);
                    var requestStats = stats.getOrDefault(key, new RequestStats());
                    requestStats.addRequest(time);
                    stats.putIfAbsent(key, requestStats);

                    if(key.requestKind() == RequestKind.STATIC) {
                        var staticRequestStats = stats.getOrDefault(staticStatsKey, new RequestStats());
                        staticRequestStats.addRequest(time);
                        stats.putIfAbsent(staticStatsKey, staticRequestStats);
                    } else if (key.requestKind() == RequestKind.DYNAMIC) {
                        var dynamicRequestStats = stats.getOrDefault(dynamicStatsKey, new RequestStats());
                        dynamicRequestStats.addRequest(time);
                        stats.putIfAbsent(dynamicStatsKey, dynamicRequestStats);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stats;
    }

    private static void printStatistics(Map<RequestKey, RequestStats> stats) {
        System.out.println("HTTP Method | URI | Total Requests | Total Time(ms) | Avg Time(ms) | Variant(ms)");
        System.out.println("------------------------------------------------------------------");
        stats.forEach((key, value) -> {
            System.out.printf(
                    "%-10s | %-40s | %-14d | %-13.4f | %.4f | %.4f%n",
                    key.method(),
                    key.uri(),
                    value.getCount(),
                    value.getTotalTime(),
                    value.getAverageTime(),
                    value.getVariance()
            );
        });
    }


}