package com.peak.authenticationservice.Util;

import com.peak.authenticationservice.Entity.Roles.Roles;
import com.peak.authenticationservice.Entity.User;
import com.peak.authenticationservice.Repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {
    @Autowired
    private UserRepository userRepository;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    //public static final String SECRET_KEY="QWRmaHVkZmhvZGpkZmhjNDZpYmtkYnVib2ppZmRwZGZzamJi";

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.privateKey=loadPrivateKey();
        this.publicKey=loadPublicKey();
        preWarmJwt();
    }

    private void preWarmJwt() {
        // Generates a dummy token on startup to force JVM class loading & JIT compilation. This is to make first request faster
        Jwts.builder()
                .subject("warmup")
                .signWith(this.privateKey)
                .compact();
    }

    public String generateToken(String username) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        User user=userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        List<String> roleNames = user.getRoles().stream()
                .map(Enum::name) // Converts Role.ROLE_USER to "ROLE_USER"
                .toList();
        Map<String, Object> claims = Map.of("roles", roleNames,"userId",user.getUserId());
        return Jwts.builder().claims(claims).subject(username).issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
                .signWith(privateKey).compact();
    }

//    private Key getKey()
//    {
//        byte[] bytes= Base64.getDecoder().decode(SECRET_KEY);
//
//        return Keys.hmacShaKeyFor(bytes);
//    }

    private static PrivateKey loadPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String key = new String(Files.readAllBytes(Paths.get("src/main/resources/private_key.pem")))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String key=new String(Files.readAllBytes(Paths.get("src/main/resources/public_key.pem")))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private Claims getClaims(String token) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
    }

    public Date extractExpiration(String token) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return getClaims(token).getExpiration();
    }

    public List extractRoles(String token) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return getClaims(token).get("roles", List.class);
    }

    public String extractUserId(String token) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return getClaims(token).get("userId", String.class);
    }

    public String extractUsername(String token) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return getClaims(token).getSubject();
    }

}
