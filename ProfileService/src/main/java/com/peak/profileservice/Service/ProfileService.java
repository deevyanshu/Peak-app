package com.peak.profileservice.Service;

import com.peak.profileservice.Dto.CreateProfileRequestDto;
import com.peak.profileservice.Dto.ProfileResponseDto;

public interface ProfileService {

    public ProfileResponseDto createProfile(String userId, CreateProfileRequestDto createProfileRequestDto);

    public String updateProfilePic(String userId, String profilePicUrl);

    public String updateProfileBio(String userId, String profileBio);

    public void saveProfilePic(String userId, String profilePicUrl);

    public String getProfilePicUrl(String userId);
}
