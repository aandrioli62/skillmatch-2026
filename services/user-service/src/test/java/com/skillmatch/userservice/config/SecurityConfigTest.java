package com.skillmatch.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code filterChain()} bean is already exercised end-to-end by
 * {@code UserServiceIntegrationTest} (a full {@code @SpringBootTest}, not a security-stubbed
 * slice). This class targets {@code jwtAuthenticationConverter()} directly, since its inner
 * lambda (mapping the {@code realm_access} claim to Spring authorities) never runs when tests
 * use {@code SecurityMockMvcRequestPostProcessors.jwt().authorities(...)}, which injects
 * authorities without ever calling the converter.
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void jwtAuthenticationConverter_mapsRealmRolesToAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("PROFESSIONAL", "ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("kc-subject")
                .build();

        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_PROFESSIONAL", "ROLE_ADMIN");
    }

    @Test
    void jwtAuthenticationConverter_missingRealmAccessClaim_yieldsNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject("kc-subject")
                .build();

        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }
}
