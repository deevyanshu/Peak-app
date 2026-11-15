package com.peak.gateway.Filter;

import com.peak.gateway.Util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtUtil jwtUtil;

    private final RedisTemplate<String,Object> redisTemplate;

    private String tokenKey="token:";

    public AuthFilter(JwtUtil jwtUtil, RedisTemplate<String,Object> redisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    public static class Config {
        // Add any configuration properties here if needed
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String headerValue = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if(headerValue == null || !headerValue.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = headerValue.substring(7); // Extract the token after "Bearer "

            try{
                ServerHttpRequest modifiedRequest;
                if (!redisTemplate.hasKey(tokenKey + token)) {
                    jwtUtil.validateToken(token);
                    Map<String,String> value=new HashMap<>();
                    value.put("userId", jwtUtil.extractUserId(token));
                    value.put("roles", String.join(",", jwtUtil.extractRoles(token)));
                    modifiedRequest= exchange.getRequest().mutate()
                            .header("userId", value.get("userId"))
                            .header("roles", value.get("roles"))
                            .build();
                    redisTemplate.opsForValue().set(tokenKey + token, value, 1000 *60 *5);
                }else {
                    @SuppressWarnings("unchecked")
                    Map<String,String> value=(Map<String,String>) redisTemplate.opsForValue().get(tokenKey + token);
                    if(value==null || value.isEmpty())
                    {
                        return onError(exchange, "Unauthorized access", HttpStatus.UNAUTHORIZED);
                    }
                    modifiedRequest= exchange.getRequest().mutate()
                            .header("userId", value.get("userId"))
                            .header("roles", value.get("roles"))
                            .build();
                }
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            }catch (Exception e){
                return onError(exchange, "Unauthorized access", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
//        exchange.getResponse().setStatusCode(httpStatus);
//        return exchange.getResponse().setComplete();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Create a JSON error payload
        String errorBody = "{\"status\":" + httpStatus.value() + ",\"error\":\"" + err + "\"}";
        byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
