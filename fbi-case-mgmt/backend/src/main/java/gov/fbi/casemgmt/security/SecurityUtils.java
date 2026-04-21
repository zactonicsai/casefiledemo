package gov.fbi.casemgmt.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Convenience accessors for the current authenticated principal.
 */
@UtilityClass
public class SecurityUtils {

    public static Optional<String> currentUsername() {
        return currentAuth().map(Authentication::getName);
    }

    public static Collection<String> currentRoles() {
        return currentAuth()
            .map(Authentication::getAuthorities)
            .orElse(List.of())
            .stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
    }

    public static Optional<Jwt> currentJwt() {
        return currentAuth()
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(a -> ((JwtAuthenticationToken) a).getToken());
    }

    public static Optional<String> currentBadgeNumber() {
        return currentJwt().map(j -> j.getClaimAsString("badgeNumber"));
    }

    public static boolean hasRole(String role) {
        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return currentRoles().contains(normalized);
    }

    private static Optional<Authentication> currentAuth() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .filter(Authentication::isAuthenticated);
    }
}
