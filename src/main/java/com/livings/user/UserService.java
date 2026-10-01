package com.livings.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livings.common.ApiException;
import com.livings.university.UniversityService;
import com.livings.user.UserDtos.UpdateProfileRequest;

@Service
public class UserService {

    private final UserRepository users;
    private final UniversityService universityService;

    public UserService(UserRepository users, UniversityService universityService) {
        this.users = users;
        this.universityService = universityService;
    }

    public User require(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User", id));
    }

    /** Partial update: null fields are left unchanged. */
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = require(userId);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.gender() != null) {
            user.setGender(request.gender());
        }
        if (request.homeCountry() != null) {
            user.setHomeCountry(request.homeCountry());
        }
        if (request.universityId() != null) {
            user.setUniversity(universityService.require(request.universityId()));
        }
        if (request.languages() != null) {
            user.setLanguages(request.languages());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return user;
    }
}
