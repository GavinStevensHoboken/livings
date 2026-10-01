package com.livings.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "livings.security.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, String issuer) {
}
