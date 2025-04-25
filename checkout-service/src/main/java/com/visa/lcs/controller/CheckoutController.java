package com.visa.lcs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.visa.lcs.dto.CheckoutRequest;
import com.visa.lcs.dto.CheckoutResponse;
import jakarta.annotation.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("checkout")
public class CheckoutController {

    @Resource
    private ObjectMapper objectMapper;

    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sessionMap = new ConcurrentHashMap<>();


    @GetMapping("/sse")
    public Flux<ServerSentEvent<String>> sse() {

        // 第一個初始化事件 - 回傳 sessionId
        String sessionId = UUID.randomUUID().toString();
        ServerSentEvent<String> initEvent = ServerSentEvent.<String>builder()
                .event("endpoint")
                .data(sessionId)
                .build();

        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionMap.put(sessionId, sink);

        // 心跳事件流
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(10))
                .map(i -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("ping")
                        .build());


        Flux<ServerSentEvent<String>> eventStream = sink.asFlux()
                .doFinally(signal -> sessionMap.remove(sessionId));

        // 合併所有流（init ➝ heartbeat + 推播）
        return Flux.concat(Flux.just(initEvent), Flux.merge(heartbeat, eventStream));
    }


    @PostMapping("/pay/{sessionId}")
    public Mono<ResponseEntity<String>> postHello(@RequestHeader HttpHeaders headers, @PathVariable String sessionId, @RequestBody CheckoutRequest request) throws JsonProcessingException {

        String message = request.getMessage();
        // 這裡可以根據需要處理請求，然後將結果發送到 SSE 客戶端
        List<String> requestIdHeaders = headers.get("RequestId");
        if (requestIdHeaders == null || requestIdHeaders.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body("Missing requestId header"));
        }

        String requestId = requestIdHeaders.get(0);

        var sink = sessionMap.get(sessionId);
        if (sink != null) {

            CheckoutResponse checkoutResponse = CheckoutResponse.builder()
                    .message(sessionId + ": " + requestId + "-" + message)
                    .requestId(requestId)
                    .build();

            String jsonData = objectMapper.writeValueAsString(checkoutResponse);
            sink.tryEmitNext(ServerSentEvent.<String>builder()
                    .event("data")
                    .data(jsonData)
                    .build());
            return Mono.just(ResponseEntity.ok("Message sent to session " + sessionId));
        } else {
            return Mono.just(ResponseEntity.status(404).body("No SSE connection for sessionId: " + sessionId));
        }
    }
}
