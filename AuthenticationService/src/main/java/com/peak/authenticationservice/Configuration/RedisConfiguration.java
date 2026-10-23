package com.peak.authenticationservice.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Getter
@Setter
public class RedisConfiguration {

    private String password;
    private SentinelProperties sentinel;

    @Getter
    @Setter
    public static class SentinelProperties {
        private String master;
        private List<String> nodes;
    }

//    @Bean
//    public LettuceConnectionFactory redisConnectionFactory() {
//        // 1. Configure Sentinel Topology
//        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
//        sentinelConfig.setMaster(sentinel.getMaster());
//
//        // Register each Sentinel node
//        if (sentinel.getNodes() != null) {
//            sentinel.getNodes().forEach(node -> {
//                String[] parts = node.split(":");
//                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
//            });
//        }
//
//        sentinelConfig.setPassword(password);
//
//        // this is because the app is not containerized and the redis is containerized, so the app cannot resolve the redis hostname, so we need to resolve it to localhost
//        MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(
//                new DnsResolver() {
//                    @Override
//                    public InetAddress[] resolve(String host) throws UnknownHostException {
//                        // Route all resolved hosts (e.g., redis-master, 172.24.0.4) to 127.0.0.1
//                        return new InetAddress[]{ InetAddress.getByName("127.0.0.1") };
//                    }
//                },
//                hostAndPort -> hostAndPort // leaves host/port unchanged after DNS resolution
//        );
//
//        ClientResources clientResources = DefaultClientResources.builder()
//                .socketAddressResolver(resolver)
//                .build();
//
//        // 2. Configure Lettuce with Read/Write Splitting strategy
//        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//                // REPLICA_PREFERRED ensures reads hit replicas, writes hit Master dynamically.
//                // In a Sentinel setup, if the Master fails, Lettuce automatically re-routes
//                // writes to the newly elected Master and reads to the remaining active replicas.
//                .readFrom(ReadFrom.REPLICA_PREFERRED)
//                .clientResources(clientResources)
//                .build();
//
//        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
//    }

    @Bean
    public ClientResources clientResources() {
        // Force Lettuce to map internal Docker container IPs/hostnames back to host localhost (127.0.0.1)
        MappingSocketAddressResolver resolver = MappingSocketAddressResolver.create(
                new DnsResolver() {
                    @Override
                    public InetAddress[] resolve(String host) throws UnknownHostException {
                        return new InetAddress[]{ InetAddress.getByName("127.0.0.1") };
                    }
                },
                hostAndPort -> hostAndPort
        );

        return DefaultClientResources.builder()
                .socketAddressResolver(resolver)
                .build();
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        sentinelConfig.setMaster(sentinel.getMaster());

        if (sentinel.getNodes() != null) {
            sentinel.getNodes().forEach(node -> {
                String[] parts = node.split(":");
                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
            });
        }
        sentinelConfig.setPassword(password);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .clientResources(clientResources)
                .build();

        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key and HashKey Serializer
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        JacksonJsonRedisSerializer<Object> jacksonSerializer = new JacksonJsonRedisSerializer<>(Object.class);

        //RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // this is for the rate limiter, we need to create a ProxyManager bean that will be used by the RateLimiterService to manage the buckets in Redis for the rate limiting of OTP generation
//    @Bean
//    public ProxyManager<String> proxyManager(LettuceConnectionFactory connectionFactory) {
//        RedisClient redisClient=(RedisClient) connectionFactory.getNativeClient();
//        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
//                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
//        );
//        // Automatically evict bucket metadata from Redis after 10 minutes of inactivity
//        return LettuceBasedProxyManager.builderFor(connection)
//                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
//                .build();
//    }

    @Bean
    public ProxyManager<String> proxyManager(ClientResources clientResources) {
        // Build the Lettuce Sentinel URI for Bucket4j
        RedisURI.Builder builder = RedisURI.builder()
                .withHost("127.0.0.1")
                .withPort(6379);

        if (password != null && !password.isEmpty()) {
            builder.withPassword(password.toCharArray());
        }

        RedisClient client = RedisClient.create(builder.build());
        StatefulRedisConnection<String, byte[]> connection = client.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );

        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
    }
}
