package com.livings.security;

import com.livings.user.Role;

/** Authenticated principal extracted from the JWT; inject with {@code @AuthenticationPrincipal}. */
public record AuthUser(Long id, String email, Role role) {
}
