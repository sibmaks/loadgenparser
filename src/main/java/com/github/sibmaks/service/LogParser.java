package com.github.sibmaks.service;

import com.github.sibmaks.bus.EventPublisher;
import com.github.sibmaks.dto.Request;
import com.github.sibmaks.dto.RequestKey;
import com.github.sibmaks.dto.RequestKind;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;

public class LogParser {
    public static final String RQ_TOPIC = "request";
    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
            "^\\[\\d+]\\[(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)].*?at (\\d+[.,]\\d+)ms.*?(http://\\S+)$"
    );
    private final EventPublisher eventPublisher;


    public LogParser(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private static boolean isStaticURI(String uri) {
        return uri.contains("/img/") || uri.contains("/cmsstatic/") || uri.contains("/js/");
    }

    public void parse(String fileName, int lastRequestIndex) throws IOException {
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

                eventPublisher.publish(RQ_TOPIC, rq);
                if (lastRequestIndex <= requestIndex++) {
                    break;
                }
            }
        }
    }
}
