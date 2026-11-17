package com.peak.profileservice.Service.Implementation;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.peak.profileservice.Dto.CreateProfileRequestDto;
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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final Storage storage;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Override
    @Transactional
    public void createProfile(String userId, CreateProfileRequestDto createProfileRequestDto) {
        Profile profile = new Profile();
        profile.setUserId(userId);
        profile.setProfileName(createProfileRequestDto.profileName());
        if(createProfileRequestDto.privacy().equals("Private"))
        {
            profile.setPrivacy(Privacy.PRIVATE);
        }
        profileRepository.save(profile);
    }

    @Override
    @Transactional
    public void updateProfilePic(String userId, String contentType) {
        // Define a unique path for the file inside the bucket
        Profile profile= profileRepository.findById(userId).orElseThrow(() -> new BadRequestException("Profile not found", HttpStatus.NOT_FOUND));
        String blobName = "profiles/" + userId + "/avatar.jpg";

        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();


        URL signedUrl = storage.signUrl(
                blobInfo,
                10, // Expiration time
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType(), // Correct option for enforcing content type
                Storage.SignUrlOption.withV4Signature()
        );

        // The final URL where the image can be fetched later (assuming public read via CDN/signed read, or public bucket policy for avatars)
        String publicFileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, blobName);
        profile.setProfile_pic_url(publicFileUrl);
        profileRepository.save(profile);
        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", signedUrl.toString());
        response.put("fileUrl", publicFileUrl);
    }

    @Override
    @Transactional
    public void updateProfileBio(String userId, String profileBio) {
        Profile profile= profileRepository.findById(userId).orElseThrow(() -> new BadRequestException("Profile not found", HttpStatus.NOT_FOUND));
        profile.setBio(profileBio);
        profileRepository.save(profile);
    }
}
