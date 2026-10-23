package com.peak.profileservice.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProfileRequestDto(String profile_pic_url, String bio, @NotNull @NotBlank String profileName) {
}
