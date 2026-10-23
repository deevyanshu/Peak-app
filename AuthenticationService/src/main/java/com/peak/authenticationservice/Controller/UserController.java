package com.peak.authenticationservice.Controller;

import com.peak.authenticationservice.Dto.JwtResponse;
import com.peak.authenticationservice.Dto.RefreshTokenRequestDto;
import com.peak.authenticationservice.Dto.UserRequestDto;
import com.peak.authenticationservice.Dto.UserResponseDto;
import com.peak.authenticationservice.Service.OtpService;
import com.peak.authenticationservice.Service.RefreshTokenService;
import com.peak.authenticationservice.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserController {



    /* To Do:-
    1. Jwt token access/Deny list using redis
    2. ensuring old jwt does not get access after refresh token is used before expiration
    3. Httpcookie for web based requests
    4. Oauth2
    */

    private final UserService userService;

    private final OtpService otpService;

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/generate-otp")
    public ResponseEntity<String> generateOtp(@Valid @RequestBody UserRequestDto userRequestDto) throws MethodArgumentNotValidException {
        return ResponseEntity.ok(otpService.generateOtp(userRequestDto));
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<UserResponseDto> validateOtp(@RequestParam String email,@RequestParam String otp) throws MethodArgumentNotValidException {
        return ResponseEntity.ok(otpService.validateOtp(email, otp));
    }

    @PostMapping("/generate-token")
    public ResponseEntity<JwtResponse> loginUser(@Valid @RequestBody UserRequestDto userRequestDto) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return ResponseEntity.ok(userService.loginUser(userRequestDto));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JwtResponse> refreshToken(@Valid @RequestBody RefreshTokenRequestDto refreshToken) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return ResponseEntity.ok(refreshTokenService.refreshToken(refreshToken.refreshToken()));
    }

}
