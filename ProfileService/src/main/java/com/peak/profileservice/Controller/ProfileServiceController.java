package com.peak.profileservice.Controller;

import com.peak.profileservice.Dto.CreateProfileRequestDto;
import com.peak.profileservice.Service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileServiceController {
    // Need to create:-
    /*
    1. Search service with elastic search
    2. quarantine cloud storage
    3. debezium for profile service to search service
    */
    private final ProfileService profileService;

    @PostMapping("/createProfile")
    public ResponseEntity<Void> createProfile(@RequestHeader("userId") String userId, @Valid @RequestBody CreateProfileRequestDto createProfileRequestDto) {
        profileService.createProfile(userId, createProfileRequestDto);
        return ResponseEntity.ok().build();
    }
}
