package com.peak.authenticationservice.Service;

import com.peak.authenticationservice.Dto.JwtResponse;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public interface RefreshTokenService {

    public String createRefreshToken(String userId,String username);

    public JwtResponse refreshToken(String refreshToken) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException;
}
