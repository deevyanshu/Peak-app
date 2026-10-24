package com.peak.profileservice.Service;

import com.peak.profileservice.Dto.CreateProfileRequestDto;

public interface ProfileService {

    public void createProfile(String userId, CreateProfileRequestDto createProfileRequestDto);

    public void updateProfilePic(String userId, String profilePicUrl);

    public void updateProfileBio(String userId, String profileBio);

}
