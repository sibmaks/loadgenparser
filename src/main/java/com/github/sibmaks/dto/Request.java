package com.github.sibmaks.dto;

import java.math.BigDecimal;

public record Request(int requestIndex, RequestKey key, BigDecimal time, long timestamp) {

}