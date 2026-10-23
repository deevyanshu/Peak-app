package com.peak.authenticationservice.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequestDto(@NotNull(message = "Username cannot be null")
                             @NotBlank(message = "Username cannot be blank")
                             @Email(message = "Username must be a valid email address")String username, @NotNull(message = "Password cannot be null")
@NotBlank(message = "Password cannot be blank")
@Size(min = 8, message = "Password must be at least 8 characters long")String password) {
}
