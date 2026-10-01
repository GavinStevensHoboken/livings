package com.livings.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livings.security.AuthUser;
import com.livings.user.UserDtos.PublicUserResponse;
import com.livings.user.UserDtos.UpdateProfileRequest;
import com.livings.user.UserDtos.UserResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return UserResponse.from(userService.require(me.id()));
    }

    @PatchMapping("/me")
    @Transactional
    public UserResponse updateMe(@AuthenticationPrincipal AuthUser me,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(userService.updateProfile(me.id(), request));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PublicUserResponse get(@PathVariable Long id) {
        return PublicUserResponse.from(userService.require(id));
    }
}
