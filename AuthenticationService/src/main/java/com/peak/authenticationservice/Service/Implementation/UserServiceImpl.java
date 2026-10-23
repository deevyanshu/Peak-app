package com.peak.authenticationservice.Service.Implementation;

import com.peak.authenticationservice.Dto.JwtResponse;
import com.peak.authenticationservice.Dto.UserRequestDto;
import com.peak.authenticationservice.Dto.UserResponseDto;
import com.peak.authenticationservice.Entity.Roles.Roles;
import com.peak.authenticationservice.Entity.User;
import com.peak.authenticationservice.ExceptionHandler.BadRequestException;
import com.peak.authenticationservice.Repository.UserRepository;
import com.peak.authenticationservice.Service.OtpService;
import com.peak.authenticationservice.Service.RefreshTokenService;
import com.peak.authenticationservice.Service.UserService;
import com.peak.authenticationservice.Util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponseDto registerUser(UserRequestDto user) {
        User user1=new User();
        user1.setUsername(user.username());
        user1.setPassword(passwordEncoder.encode(user.password()));
        user1.setRoles(List.of(Roles.USER));
        userRepository.save(user1);
        return new UserResponseDto(user1.getUsername(), user1.getRoles());
    }

    @Override
    public JwtResponse loginUser(UserRequestDto user) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.username(), user.password())
            );
        } catch (Exception e) {
            log.error("Actual exception:"+e.getClass().getName());
            throw new BadRequestException("Invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        String token=jwtUtil.generateToken(user.username());
        String refreshToken=refreshTokenService.createRefreshToken(jwtUtil.extractUserId(token), jwtUtil.extractUsername(token));
        return new JwtResponse(token,"Bearer",jwtUtil.extractUserId(token),jwtUtil.extractRoles(token), jwtUtil.extractExpiration(token),refreshToken);
    }

    @Override
    public Boolean checkUserExists(String username) {
        return userRepository.existsByUsername(username);
    }
}
