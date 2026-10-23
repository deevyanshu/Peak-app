package com.peak.authenticationservice.Dto;

import com.peak.authenticationservice.Entity.Roles.Roles;

import java.util.Date;
import java.util.List;

public record JwtResponse(String token, String type, String userId, List<String> roles, Date validUntil, String refreshToken) {
}
