package com.github.sibmaks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class RequestStats {
    private final List<BigDecimal> values = new ArrayList<>();

    private BigDecimal totalTime = BigDecimal.ZERO;

    public void addRequest(BigDecimal time) {
        time = time.setScale(12, RoundingMode.HALF_UP);
        totalTime = totalTime.add(time);
        values.add(time);
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
}