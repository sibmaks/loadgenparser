package com.github.sibmaks;

import com.github.sibmaks.bus.InMemoryEventBus;
import com.github.sibmaks.dto.Request;
import com.github.sibmaks.dto.RequestKey;
import com.github.sibmaks.dto.RequestKind;
import com.github.sibmaks.service.ExcelWriter;
import com.github.sibmaks.service.LogParser;
import picocli.CommandLine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.github.sibmaks.service.LogParser.RQ_TOPIC;

public class Application implements Runnable {
    @CommandLine.Option(names = {"-f", "--file"}, description = "Input log file", required = true)
    private String file;
    @CommandLine.Option(names = {"-t", "--to"}, description = "Amount of request to read", defaultValue = "-1")
    private int lastRequestIndex;
    @CommandLine.Option(names = {"-s", "--step"}, description = "Step to collect statistic", defaultValue = "-1")
    private int step;

    public static void main(String[] args) {
        var commandLine = new CommandLine(new Application());
        commandLine.execute(args);
    }

    private static void collectRPS(InMemoryEventBus bus, Map<Long, Integer> rpsStats) {
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            var minute = (rq.timestamp() / 1000 / 60) * 60;
            rpsStats.put(minute, rpsStats.getOrDefault(minute, 0) + 1);
        });
    }

    private static void collectAll(Map<RequestKey, RequestStats> stats, InMemoryEventBus bus) {
        var genericKey = new RequestKey("ALL", RequestKind.ALL);
        var genericRequestStats = stats.computeIfAbsent(genericKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            genericRequestStats.addRequest(rq);
        });
    }

    private static void collectSpecificType(String requestKey,
                                            RequestKind dynamic,
                                            Map<RequestKey, RequestStats> stats,
                                            InMemoryEventBus bus) {
        var dynamicKey = new RequestKey(requestKey, dynamic);
        var dynamicRequestStats = stats.computeIfAbsent(dynamicKey, it -> new RequestStats());
        bus.subscribe(RQ_TOPIC, it -> {
            var rq = (Request) it;
            if (rq.key().requestKind() != dynamic) {
                return;
            }
            dynamicRequestStats.addRequest(rq);
        });
    }

    @Override
    public void run() {
        var stats = new LinkedHashMap<RequestKey, RequestStats>();
        var rpsStats = new LinkedHashMap<Long, Integer>();

        var bus = new InMemoryEventBus();

        collectRPS(bus, rpsStats);
        collectAll(stats, bus);
        collectSpecificType("STATIC", RequestKind.STATIC, stats, bus);
        collectSpecificType("DYNAMIC", RequestKind.DYNAMIC, stats, bus);

        if (step > 0) {
            setUpStepStatistic(stats, bus);
        }

        try {
            var parser = new LogParser(bus);
            parser.parse(file, lastRequestIndex);

            var writer = new ExcelWriter();
            writer.write(stats, rpsStats, "output-%d.xlsx".formatted(System.currentTimeMillis()));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void setUpStepStatistic(Map<RequestKey, RequestStats> stats, InMemoryEventBus bus) {
        bus.subscribe(RQ_TOPIC, new Consumer<>() {
            final Map<Integer, RequestStats> allStats = new LinkedHashMap<>();
            final Map<Integer, RequestStats> staticStats = new LinkedHashMap<>();
            final Map<Integer, RequestStats> dynamicStats = new LinkedHashMap<>();
            final int currentStep = step;
            final int lastIndex = lastRequestIndex;

            @Override
            public void accept(Object it) {
                var rq = (Request) it;
                var index = rq.requestIndex() - 1;
                if (lastIndex != -1 && index > lastIndex) return;

                var batch = index / currentStep;
                var threshold = (batch + 1) * currentStep;

                var allStats = this.allStats.computeIfAbsent(
                        batch,
                        b -> getRequestStats("ALL_%d", threshold, b, stats, RequestKind.ALL)
                );
                allStats.addRequest(rq);

                if (rq.key().requestKind() == RequestKind.STATIC) {
                    var requestStats = staticStats.computeIfAbsent(
                            batch,
                            b -> getRequestStats("STATIC_%d", threshold, b, stats, RequestKind.STATIC)
                    );
                    requestStats.addRequest(rq);
                }

                if (rq.key().requestKind() == RequestKind.DYNAMIC) {
                    var requestStats = staticStats.computeIfAbsent(
                            batch,
                            b -> getRequestStats("DYNAMIC_%d", threshold, b, stats, RequestKind.DYNAMIC)
                    );
                    requestStats.addRequest(rq);
                }
            }
        });
    }

    private RequestStats getRequestStats(
            String keyFormat,
            int threshold,
            Integer batch,
            Map<RequestKey, RequestStats> stats,
            RequestKind requestKind
    ) {
        var key = new RequestKey(keyFormat.formatted(threshold), requestKind);
        var requestStats = createRequestStats(keyFormat, threshold, batch, stats, requestKind);
        stats.put(key, requestStats);
        return requestStats;
    }

    private RequestStats createRequestStats(
            String keyFormat,
            int threshold,
            Integer batch,
            Map<RequestKey, RequestStats> stats,
            RequestKind requestKind
    ) {
        if (batch == 0) {
            return new RequestStats();
        }
        var parentKey = new RequestKey(keyFormat.formatted(threshold - step), requestKind);
        var parentStats = stats.get(parentKey);
        return parentStats.copy();
    }
}
