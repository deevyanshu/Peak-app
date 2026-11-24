package com.peak.profileservice.Configuration;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Getter
@Setter
public class RedisConfig {
    private String password;
    private SentinelProperties sentinel;

    @Getter
    @Setter
    public static class SentinelProperties {
        private String master;
        private java.util.List<String> nodes;
    }

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

        //JacksonJsonRedisSerializer<Object> jacksonSerializer = new JacksonJsonRedisSerializer<>(Object.class);

        // Define a type validator for safe polymorphic serialization/deserialization
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        // Build the modern GenericJacksonJsonRedisSerializer with default typing enabled
        GenericJacksonJsonRedisSerializer jacksonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();

        //RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
