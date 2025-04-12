package com.github.sibmaks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please provide log file path as argument");
            return;
        }

        Map<RequestKey, RequestStats> stats = new HashMap<>();
        Pattern logPattern = Pattern.compile(
                "^\\[(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)\\].*?at\\s(\\d+,\\d+)ms.*?(http://\\S+)"
        );

        try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = logPattern.matcher(line);
                if (matcher.find()) {
                    String method = matcher.group(1);
                    double time = Double.parseDouble(matcher.group(2).replace(',', '.'));
                    String uri = matcher.group(3);

                    RequestKey key = new RequestKey(method, uri);
                    RequestStats requestStats = stats.getOrDefault(key, new RequestStats());
                    requestStats.addRequest(time);
                    stats.putIfAbsent(key, requestStats);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        printStatistics(stats);
    }

    private static void printStatistics(Map<RequestKey, RequestStats> stats) {
        System.out.println("HTTP Method | URI | Total Requests | Total Time(ms) | Avg Time(ms)");
        System.out.println("------------------------------------------------------------------");
        stats.forEach((key, value) -> {
            System.out.printf(
                    "%-10s | %-40s | %-14d | %-13.2f | %.2f%n",
                    key.method(),
                    key.uri(),
                    value.getCount(),
                    value.getTotalTime(),
                    value.getAverageTime()
            );
        });
    }

    private record RequestKey(String method, String uri) {}

    private static class RequestStats {
        private int count = 0;
        private double totalTime = 0.0;

        public void addRequest(double time) {
            count++;
            totalTime += time;
        }

        public int getCount() {
            return count;
        }

        public double getTotalTime() {
            return totalTime;
        }

        public double getAverageTime() {
            return count > 0 ? totalTime / count : 0.0;
        }
    }
}