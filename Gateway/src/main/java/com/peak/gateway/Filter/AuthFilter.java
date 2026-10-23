package com.peak.gateway.Filter;

import com.peak.gateway.Util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtUtil jwtUtil;

    public AuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
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
                jwtUtil.validateToken(token);

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("userId", jwtUtil.extractUserId(token))
                        .header("roles", String.join(",", jwtUtil.extractRoles(token)))
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            }catch (Exception e){
                return onError(exchange, "Unauthorized access", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }
}
