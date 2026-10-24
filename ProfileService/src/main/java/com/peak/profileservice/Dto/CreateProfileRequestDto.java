package com.peak.profileservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProfileRequestDto(@NotNull(message = "Profile name is required") @NotBlank(message = "Profile name cannot be blank") String profileName,
                                      @NotNull(message = "Privacy is required") @NotBlank(message = "Privacy cannot be blank") String privacy) {
}
