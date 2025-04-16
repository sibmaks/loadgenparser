package com.github.sibmaks;

import com.github.sibmaks.dto.Request;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RequestStats {
    private final List<BigDecimal> values = new ArrayList<>();

    private BigDecimal totalTime = BigDecimal.ZERO;
    private long minTimestamp = Long.MAX_VALUE;
    private long maxTimestamp = Long.MIN_VALUE;

    public void addRequest(Request rq) {
        var time = rq.time().setScale(12, RoundingMode.HALF_UP);
        totalTime = totalTime.add(time);
        values.add(time);

        minTimestamp = Math.min(minTimestamp, rq.timestamp());
        maxTimestamp = Math.max(maxTimestamp, rq.timestamp());
    }

    public int getCount() {
        return values.size();
    }

    public BigDecimal getTotalTime() {
        return totalTime;
    }

    public BigDecimal getAverageTime() {
        var n = values.size();
        if (n < 2) return BigDecimal.ZERO;

        return totalTime.divide(BigDecimal.valueOf(n), RoundingMode.HALF_DOWN);
    }

    public BigDecimal getVariance() {
        var n = values.size();
        if (n < 2) return BigDecimal.ZERO;

        var sum = BigDecimal.ZERO;
        var sumSq = BigDecimal.ZERO;

        for (var x : values) {
            sum = sum.add(x);
            sumSq = sumSq.add(x.pow(2));
        }

        var mean = sum.divide(BigDecimal.valueOf(n), RoundingMode.HALF_DOWN);
        return sumSq.divide(BigDecimal.valueOf(n), RoundingMode.HALF_DOWN)
                .subtract(mean.pow(2));
    }

    public BigDecimal getPercentile90() {
        return getPercentile(0.90);
    }

    public BigDecimal getPercentile95() {
        return getPercentile(0.95);
    }

    public BigDecimal getPercentile99() {
        return getPercentile(0.99);
    }

    private BigDecimal getPercentile(double x) {
        var n = values.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }

        var sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        var index = (int) Math.ceil(x * n) - 1;
        index = Math.min(index, n - 1);

        return sorted.get(index);
    }

    public BigDecimal getMin() {
        var n = values.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        return values.stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal getMax() {
        var n = values.size();
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        return values.stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public long getRPS() {
        var n = values.size();
        if (n == 0) {
            return 0;
        }
        return 1000L * n / (maxTimestamp - minTimestamp);
    }
}
