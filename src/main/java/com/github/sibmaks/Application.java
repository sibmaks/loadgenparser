package com.github.sibmaks;

import com.github.sibmaks.bus.InMemoryEventBus;
import com.github.sibmaks.dto.Request;
import com.github.sibmaks.dto.RequestKey;
import com.github.sibmaks.dto.RequestKind;
import com.github.sibmaks.service.ExcelWriter;
import com.github.sibmaks.service.LogParser;

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.github.sibmaks.service.LogParser.RQ_TOPIC;

public class Application {


    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please provide log file path as argument");
            return;
        }

        var fileName = args[0];
        var stats = new LinkedHashMap<RequestKey, RequestStats>();

        var bus = new InMemoryEventBus();

        collectAll(stats, bus);

        collectSpecificType("STATIC", RequestKind.STATIC, stats, bus);

        collectSpecificType("DYNAMIC", RequestKind.DYNAMIC, stats, bus);

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
                    if (rq.key().requestIndex() > batchIndex * step) {
                        return;
                    }
                    batchRequestStats.addRequest(rq.time());
                });

                var batchStaticKey = new RequestKey(0, "STATIC_%d".formatted(i * step), RequestKind.STATIC);
                var batchStaticRequestStats = stats.computeIfAbsent(batchStaticKey, it -> new RequestStats());
                bus.subscribe(RQ_TOPIC, it -> {
                    var rq = (Request) it;
                    if (rq.key().requestIndex() > batchIndex * step || rq.key().requestKind() != RequestKind.STATIC) {
                        return;
                    }
                    batchStaticRequestStats.addRequest(rq.time());
                });

                var batchDynamicKey = new RequestKey(0, "DYNAMIC_%d".formatted(i * step), RequestKind.DYNAMIC);
                var batchDynamicRequestStats = stats.computeIfAbsent(batchDynamicKey, it -> new RequestStats());
                bus.subscribe(RQ_TOPIC, it -> {
                    var rq = (Request) it;
                    if (rq.key().requestIndex() > batchIndex * step || rq.key().requestKind() != RequestKind.DYNAMIC) {
                        return;
                    }
                    batchDynamicRequestStats.addRequest(rq.time());
                });
            }
        }

        var parser = new LogParser(bus);
        parser.parse(fileName, lastRequestIndex);

        var writer = new ExcelWriter();
        writer.write(stats, "output-%d.xlsx".formatted(System.currentTimeMillis()));
    }

    private static void collectAll(LinkedHashMap<RequestKey, RequestStats> stats, InMemoryEventBus bus) {
        var genericKey = new RequestKey(0, "ALL", RequestKind.GENERIC);
        var genericRequestStats = stats.computeIfAbsent(genericKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            genericRequestStats.addRequest(rq.time());
        });
    }

    private static void collectSpecificType(String DYNAMIC, RequestKind dynamic, LinkedHashMap<RequestKey, RequestStats> stats, InMemoryEventBus bus) {
        var dynamicKey = new RequestKey(0, DYNAMIC, dynamic);
        var dynamicRequestStats = stats.computeIfAbsent(dynamicKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            if (rq.key().requestKind() != dynamic) {
                return;
            }
            dynamicRequestStats.addRequest(rq.time());
        });
    }

}