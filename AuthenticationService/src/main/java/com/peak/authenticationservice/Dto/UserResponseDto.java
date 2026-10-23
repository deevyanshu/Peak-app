package com.peak.authenticationservice.Dto;

import com.peak.authenticationservice.Entity.Roles.Roles;

import java.util.List;

public record UserResponseDto(String username, List<Roles> roles) {
}
