package com.peak.authenticationservice.Service;

import com.peak.authenticationservice.Dto.JwtResponse;
import com.peak.authenticationservice.Dto.UserRequestDto;
import com.peak.authenticationservice.Dto.UserResponseDto;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;


public interface UserService {

    public UserResponseDto registerUser(UserRequestDto user) throws MethodArgumentNotValidException;

    public JwtResponse loginUser(UserRequestDto user) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException;

    public Boolean checkUserExists(String username);
}
