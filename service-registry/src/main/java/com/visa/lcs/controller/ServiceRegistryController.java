package com.visa.lcs.controller;

import com.visa.lcs.service.DeferredResponseManager;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
@RequestMapping("/api")
public class ServiceRegistryController {

    @Resource
    private ConcurrentLinkedDeque<String> sessionQueue;

    @Resource
    private DeferredResponseManager responseManager;

    private final WebClient webClient = WebClient.create();

    @PostMapping(value = "/do", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> doSomething(@RequestBody Map<String, Object> body) {
        String requestId = responseManager.newRequestId();

        String sessionId = sessionQueue.peekFirst();
        // 發送給 Service B，非同步不等待
        webClient.post()
                .uri("http://localhost:8081/checkout/pay/" + sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .header("RequestId", requestId)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe();

        return responseManager.createDeferred(requestId);
    }
}
