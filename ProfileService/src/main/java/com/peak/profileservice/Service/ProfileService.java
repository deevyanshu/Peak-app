package com.peak.profileservice.Service;

public interface ProfileService {

    public void createProfile(String userId, String profileName, String privacy);

    public void updateProfilePic(String userId, String profilePicUrl);

    public void updateProfileBio(String userId, String profileBio);

}
