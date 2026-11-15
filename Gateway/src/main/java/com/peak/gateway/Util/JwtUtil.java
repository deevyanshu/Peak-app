package com.peak.gateway.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {


    private PublicKey publicKey;

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.publicKey=loadPublicKey();
    }

    //public static final String SECRET_KEY="QWRmaHVkZmhvZGpkZmhjNDZpYmtkYnVib2ppZmRwZGZzamJi";

//    private Key getKey()
//    {
//        byte[] bytes= Base64.getDecoder().decode(SECRET_KEY);
//
//        return Keys.hmacShaKeyFor(bytes);
//    }

    private static PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        ClassPathResource resource = new ClassPathResource("public_key.pem");
        try (InputStream inputStream = resource.getInputStream()) {
            String key = new String(inputStream.readAllBytes())
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
    }

    public void validateToken(String token) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
    }

    private Claims getClaims(String token) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
    }

    public Date extractExpiration(String token) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return getClaims(token).getExpiration();
    }

    public List extractRoles(String token) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return getClaims(token).get("roles", List.class);
    }

    public String extractUserId(String token) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        return getClaims(token).get("userId", String.class);
    }

}
