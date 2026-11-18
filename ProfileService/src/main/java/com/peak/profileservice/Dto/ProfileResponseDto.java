package com.peak.profileservice.Dto;

import com.peak.profileservice.Entity.Privacy;

public record ProfileResponseDto(String profileName, String bio, String profilePicUrl, Privacy privacy) {
}
