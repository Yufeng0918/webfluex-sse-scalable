package com.visa.lcs;

import com.visa.lcs.service.DeferredResponseManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ConcurrentLinkedDeque;


@SpringBootApplication
public class SpringServiceRegistryApplication {

    @Bean
    public DeferredResponseManager deferredResponseManager() {
        return new DeferredResponseManager();
    }


    @Bean
    public ConcurrentLinkedDeque<String> sessionQueue() {
        return new ConcurrentLinkedDeque<>();
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringServiceRegistryApplication.class, args);
    }
}