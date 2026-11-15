package com.peak.authenticationservice.Service.Implementation;

import com.peak.authenticationservice.Dto.JwtResponse;
import com.peak.authenticationservice.ExceptionHandler.BadRequestException;
import com.peak.authenticationservice.Service.RefreshTokenService;
import com.peak.authenticationservice.Util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration-ms:604800000}") // 7 days in milliseconds
    private Long refreshTokenDurationMs;


    private final JwtUtil jwtUtil;

    private final RedisTemplate<String, Object> redisTemplate;

    public RefreshTokenServiceImpl(JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public String createRefreshToken(String userId, String username) {
       String refreshToken = UUID.randomUUID().toString();
        String key="refreshToken:"+refreshToken;
        Map<String,Object> value=new HashMap<>();
        value.put("userId", userId);
        value.put("username", username);
        redisTemplate.opsForValue().set(key,value, java.time.Duration.ofMillis(refreshTokenDurationMs));
        return refreshToken;
    }


    @Override
    public JwtResponse refreshToken(String refreshToken) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        @SuppressWarnings("unchecked")
        Map<String,Object> value=(Map<String,Object>) redisTemplate.opsForValue().get("refreshToken:"+refreshToken);

        if(value!=null && !value.isEmpty())
        {
            log.atInfo().log("Refreshing token for user: {}", value.get("username"));
            String userId=(String) value.get("userId");
            String username=(String) value.get("username");
            String newToken = jwtUtil.generateToken(username);
            String newRefreshToken = UUID.randomUUID().toString();
            redisTemplate.delete("refreshToken:"+refreshToken);
            redisTemplate.opsForValue().set("refreshToken:"+newRefreshToken, value, java.time.Duration.ofMillis(refreshTokenDurationMs));
            return new JwtResponse(newToken,"Bearer",jwtUtil.extractUserId(newToken),jwtUtil.extractRoles(newToken), jwtUtil.extractExpiration(newToken),newRefreshToken);
        }
        throw new BadRequestException("Refresh token was expired. Please make a new sign-in request.", HttpStatus.UNAUTHORIZED);
    }
}
