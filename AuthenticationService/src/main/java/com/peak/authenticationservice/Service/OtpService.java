package com.peak.authenticationservice.Service;

import com.peak.authenticationservice.Dto.UserRequestDto;
import com.peak.authenticationservice.Dto.UserResponseDto;
import org.springframework.web.bind.MethodArgumentNotValidException;

public interface OtpService {

    public String generateOtp(UserRequestDto userRequestDto);
    public UserResponseDto validateOtp(String username, String otp) throws MethodArgumentNotValidException;
}
