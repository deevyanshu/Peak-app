package com.peak.profileservice.Service.Implementation;

import com.google.cloud.storage.Storage;
import com.peak.profileservice.Entity.Privacy;
import com.peak.profileservice.Entity.Profile;
import com.peak.profileservice.ExceptionHandler.BadRequestException;
import com.peak.profileservice.Repository.ProfileRepository;
import com.peak.profileservice.Service.ProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final Storage storage;

    @Value("${gcp.bucket.name}")
    private final String bucketName;

    @Override
    @Transactional
    public void createProfile(String userId, String profileName, String privacy) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setProfileName(profileName);
        if(privacy.equals("Private"))
        {
            profile.setPrivacy(Privacy.PRIVATE);
        }
        profileRepository.save(profile);
    }

    @Override
    @Transactional
    public void updateProfilePic(String userId, String profilePicUrl) {

    }

    @Override
    @Transactional
    public void updateProfileBio(String userId, String profileBio) {
        Profile profile= profileRepository.findById(userId).orElseThrow(() -> new BadRequestException("Profile not found", HttpStatus.NOT_FOUND));
        profile.setBio(profileBio);
        profileRepository.save(profile);
    }
}
