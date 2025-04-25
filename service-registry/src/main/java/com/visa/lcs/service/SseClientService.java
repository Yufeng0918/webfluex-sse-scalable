package com.visa.lcs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
public class SseClientService {

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private ConcurrentLinkedDeque<String> sessionQueue;

    @Resource
    private DeferredResponseManager responseManager;

    @PostConstruct
    public void init() {
        Flux<ServerSentEvent<String>> stream = webClient.get()
                .uri("http://localhost:8081/checkout/sse") // Service B 的 SSE endpoint
                .retrieve()
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<>() {});

        stream.subscribe(event -> {
            try {

                if (event.event().equals("endpoint")) {
                    System.out.println("Received SSE without event type");
                    String sessionId = event.data();
                    sessionQueue.addLast(sessionId);
                    return;
                }

                if (event.event().equals("heartbeat")) {
                    log.info("Received heartbeat event");
                    return;
                }

                String json = event.data();
                JsonNode node = objectMapper.readTree(json);
                String requestId = node.get("requestId").asText();
                String message = node.get("message").asText();

                log.info("Received SSE for requestId: " + requestId);
                responseManager.complete(requestId, message);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, err -> {
            System.err.println("SSE connection error: " + err.getMessage());
        });
    }
}
