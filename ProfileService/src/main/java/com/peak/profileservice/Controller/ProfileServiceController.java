package com.peak.profileservice.Controller;

import com.peak.profileservice.Dto.CreateProfileRequestDto;
import com.peak.profileservice.Dto.ProfileResponseDto;
import com.peak.profileservice.Service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ProfileResponseDto> createProfile(@RequestHeader("userId") String userId, @Valid @RequestBody CreateProfileRequestDto createProfileRequestDto) {
        //return ResponseEntity.ok(profileService.createProfile(userId, createProfileRequestDto));
        return new ResponseEntity<>(profileService.createProfile(userId, createProfileRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/updateProfileBio")
    public ResponseEntity<String> updateProfileBio(@RequestHeader("userId") String userId, @RequestBody String profileBio) {
        return ResponseEntity.ok(profileService.updateProfileBio(userId, profileBio));
    }

    @PostMapping("/updateProfilePic")
    public ResponseEntity<String> updateProfilePic(@RequestHeader("userId") String userId, @RequestBody String profilePicUrl) {
        return ResponseEntity.ok(profileService.updateProfilePic(userId, profilePicUrl));
    }
}
