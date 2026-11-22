package com.peak.gateway.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/authFallback")
    public Mono<ResponseEntity<String>> authServiceFallback(){
        return Mono.just(ResponseEntity.status(503).body("Authentication Service is currently unavailable. Please try again later."));
    }

    @RequestMapping("/profileFallback")
    public Mono<ResponseEntity<String>> profileServiceFallback(){
        return Mono.just(ResponseEntity.status(503).body("Profile Service is currently unavailable. Please try again later."));
    }
}
