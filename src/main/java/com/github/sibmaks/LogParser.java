package com.github.sibmaks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class LogParser {

    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
            "^\\[\\d+]\\[(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)].*?at (\\d+\\.\\d+)ms.*?(http://\\S+)$"
    );
    private static final String RQ_TOPIC = "request";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please provide log file path as argument");
            return;
        }

        var fileName = args[0];
        var stats = new LinkedHashMap<RequestKey, RequestStats>();

        var bus = new InMemoryEventBus();

        var genericKey = new RequestKey(0, "ALL", RequestKind.GENERIC);
        var genericRequestStats = stats.computeIfAbsent(genericKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            genericRequestStats.addRequest(rq.time());
        });

        var staticKey = new RequestKey(0, "STATIC", RequestKind.STATIC);
        var staticRequestStats = stats.computeIfAbsent(staticKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            if (rq.key().requestKind() != RequestKind.STATIC) {
                return;
            }
            staticRequestStats.addRequest(rq.time());
        });

        var dynamicKey = new RequestKey(0, "DYNAMIC", RequestKind.DYNAMIC);
        var dynamicRequestStats = stats.computeIfAbsent(dynamicKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            if (rq.key().requestKind() != RequestKind.DYNAMIC) {
                return;
            }
            dynamicRequestStats.addRequest(rq.time());
        });

        var lastRequestIndex = Integer.MAX_VALUE;
        if (args.length > 1) {
            lastRequestIndex = Integer.parseInt(args[1]);
        }

        if (args.length > 2) {
            var step = Integer.parseInt(args[2]);
            var batcheCount = lastRequestIndex / step;
            for (int i = 1; i <= batcheCount; i++) {
                int batchIndex = i;
                var batchKey = new RequestKey(0, "ALL_%d".formatted(i * step), RequestKind.GENERIC);
                var batchRequestStats = stats.computeIfAbsent(batchKey, it -> new RequestStats());
                bus.subscribe(RQ_TOPIC, it -> {
                    var rq = (Request) it;
                    if(rq.key().requestIndex() > batchIndex * step) {
                        return;
                    }
                    batchRequestStats.addRequest(rq.time());
                });

                var batchStaticKey = new RequestKey(0, "STATIC_%d".formatted(i * step), RequestKind.STATIC);
                var batchStaticRequestStats = stats.computeIfAbsent(batchStaticKey, it -> new RequestStats());
                bus.subscribe(RQ_TOPIC, it -> {
                    var rq = (Request) it;
                    if(rq.key().requestIndex() > batchIndex * step || rq.key().requestKind() != RequestKind.STATIC) {
                        return;
                    }
                    batchStaticRequestStats.addRequest(rq.time());
                });

                var batchDynamicKey = new RequestKey(0, "DYNAMIC_%d".formatted(i * step), RequestKind.DYNAMIC);
                var batchDynamicRequestStats = stats.computeIfAbsent(batchDynamicKey, it -> new RequestStats());
                bus.subscribe(RQ_TOPIC, it -> {
                    var rq = (Request) it;
                    if(rq.key().requestIndex() > batchIndex * step || rq.key().requestKind() != RequestKind.DYNAMIC) {
                        return;
                    }
                    batchDynamicRequestStats.addRequest(rq.time());
                });
            }
        }

        try (var reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            var requestIndex = 1;
            while ((line = reader.readLine()) != null) {
                var matcher = LOG_LINE_PATTERN.matcher(line);
                if (!matcher.find()) {
                    continue;
                }
                var method = matcher.group(1);
                var time = new BigDecimal(matcher.group(2).replace(',', '.'));
                var uri = matcher.group(3);

                var requestKind = isStaticURI(uri) ? RequestKind.STATIC : RequestKind.DYNAMIC;
                var key = new RequestKey(
                        requestIndex,
                        method,
                        requestKind
                );
                var rq = new Request(
                        key,
                        time
                );

                bus.publish(RQ_TOPIC, rq);
                if (lastRequestIndex <= requestIndex++) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExcelWriter.writeExcelReport(stats, "output-%d.xlsx".formatted(System.currentTimeMillis()));
    }

    private static boolean isStaticURI(String uri) {
        return uri.contains("/img/") || uri.contains("/cmsstatic/") || uri.contains("/js/");
    }


}