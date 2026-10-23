package com.peak.authenticationservice.Service.RateLimiting;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class LoginLimiterService {

    private final ProxyManager<String> proxyManager;

    public Bucket resolveBucket(String key) {
        String redisKey = "rate_limit:login:" + key;
        //return cache.computeIfAbsent(key, this::createNewBucket);
        return proxyManager.builder().build(redisKey, this::createNewBucket);
    }

    private BucketConfiguration createNewBucket() {
        // Allows a max capacity of 5 tokens, refilling 5 tokens every 10 minutes.
        Bandwidth limit = Bandwidth.builder()
                .capacity(2)
                .refillIntervally(3, Duration.ofMinutes(10))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}
