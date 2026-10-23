package com.peak.authenticationservice.Service.Implementation;

import com.peak.authenticationservice.Dto.UserRequestDto;
import com.peak.authenticationservice.Dto.UserResponseDto;
import com.peak.authenticationservice.ExceptionHandler.BadRequestException;
import com.peak.authenticationservice.Service.EmailService;
import com.peak.authenticationservice.Service.OtpService;
import com.peak.authenticationservice.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;
    private final EmailService emailService;
    private static final String OTP_PREFIX = "OTP:";
    private static final long OTP_EXPIRE_MINUTES = 5;
    private static final int MAX_RETRIES=3;


    @Override
    public String generateOtp(UserRequestDto userRequestDto) {
        // later need to add redis for this for faster performance and to avoid multiple requests for same user
        if(userService.checkUserExists(userRequestDto.username())){
            throw new BadRequestException("User already exists with this username", HttpStatus.BAD_REQUEST);
        }
        // Generate secure 6-digit OTP
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));
        String otpKey=userRequestDto.username();
        String retryKey=OTP_PREFIX+":retries:"+otpKey;
        String dataKey=OTP_PREFIX+":data:"+otpKey;
        redisTemplate.opsForValue().set(OTP_PREFIX+otpKey,otp, Duration.ofMinutes(OTP_EXPIRE_MINUTES));
        redisTemplate.opsForValue().set(retryKey,"0",Duration.ofMinutes(OTP_EXPIRE_MINUTES));
        redisTemplate.opsForValue().set(dataKey,userRequestDto,Duration.ofMinutes(OTP_EXPIRE_MINUTES));
        emailService.sendOtpEmail(userRequestDto.username(), otp);
        return "OTP Sent successfully";
    }

    @Override
    public UserResponseDto validateOtp(String username, String otp) throws MethodArgumentNotValidException {
        String otpKey=OTP_PREFIX+username;
        String retryKey=OTP_PREFIX+":retries:"+username;
        String dataKey=OTP_PREFIX+":data:"+username;
        String storedOtp = (String) redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            throw new BadRequestException("OTP has expired or does not exist for the provided username.", HttpStatus.BAD_REQUEST);
        }
        if (!storedOtp.equals(otp)) {
            // Increment retry count
            int retries = Integer.parseInt((String) redisTemplate.opsForValue().get(retryKey));
            retries++;
            redisTemplate.opsForValue().set(retryKey,String.valueOf(retries),Duration.ofMinutes(OTP_EXPIRE_MINUTES));
            if (retries >= MAX_RETRIES) {
                // Invalidate OTP after max retries
                redisTemplate.delete(otpKey);
                redisTemplate.delete(retryKey);
                redisTemplate.delete(dataKey);
                throw new BadRequestException("Maximum retries exceeded. OTP invalidated.", HttpStatus.TOO_MANY_REQUESTS);
            }
            throw new BadRequestException("Invalid OTP. Please try again.", HttpStatus.BAD_REQUEST);
        }
        // OTP is valid, delete it from Redis
        redisTemplate.delete(otpKey);
        redisTemplate.delete(retryKey);
        UserRequestDto userRequestDto = (UserRequestDto) redisTemplate.opsForValue().get(dataKey);
        redisTemplate.delete(dataKey);
        return userService.registerUser(userRequestDto);
    }
}
