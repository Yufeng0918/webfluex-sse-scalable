package com.visa.lcs.service;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public class DeferredResponseManager {

    private final Map<String, MonoSink<String>> pendingRequests = new ConcurrentHashMap<>();

    public Mono<String> createDeferred(String requestId) {
        return Mono.create(sink -> {
            pendingRequests.put(requestId, sink);

            // Optional: 超時清理
            Mono.delay(Duration.ofSeconds(15)).subscribe(x -> {
                if (pendingRequests.remove(requestId) != null) {
                    sink.error(new TimeoutException("Request timed out"));
                }
            });
        });
    }

    public void complete(String requestId, String data) {
        MonoSink<String> sink = pendingRequests.remove(requestId);
        if (sink != null) {
            sink.success(data);
        }
    }

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
