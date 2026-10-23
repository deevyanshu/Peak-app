package com.peak.authenticationservice.Service.RateLimiting;

import com.peak.authenticationservice.Dto.UserRequestDto;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginLimitFilter extends OncePerRequestFilter {

    private final LoginLimiterService loginLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("/api/v1/auth/generate-token".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {

            // Key by client IP address (or use email parameter if available)
            EagerCachedRequestWrapper wrappedRequest=new EagerCachedRequestWrapper(request);
            String clientKey = getClientId(wrappedRequest);
            Bucket bucket = loginLimiterService.resolveBucket(clientKey);

            // Consume 1 token from the bucket
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (probe.isConsumed()) {
                // Pass remaining tokens count in response header
                response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
                filterChain.doFilter(wrappedRequest, response);
            } else {
                // Return 429 Too Many Requests if bucket is empty
                long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many login requests. Please try again later.\"}");
                return;
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String getClientId(EagerCachedRequestWrapper request) {
        try{
            ObjectMapper objectMapper=new ObjectMapper();
            UserRequestDto userRequestDto=objectMapper.readValue(request.getInputStream(), UserRequestDto.class);
            return userRequestDto.username();
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
