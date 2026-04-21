package gov.fbi.casemgmt.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts a Keycloak JWT into a Spring {@link AbstractAuthenticationToken}.
 * <p>
 * Extracts realm-level roles from {@code realm_access.roles} and promotes them
 * to Spring {@code GrantedAuthority} values. Also merges any client-scoped roles.
 */
@Component
public class JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String CLIENT_ID = "Demo Only-web";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                extractRealmRoles(jwt),
                extractClientRoles(jwt)
        ).distinct().collect(Collectors.toList());

        String principal = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    @SuppressWarnings("unchecked")
    private Stream<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
        if (realmAccess == null) return Stream.empty();
        List<String> roles = (List<String>) realmAccess.getOrDefault(ROLES, List.of());
        return roles.stream().map(this::toAuthority);
    }

    @SuppressWarnings("unchecked")
    private Stream<GrantedAuthority> extractClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
        if (resourceAccess == null) return Stream.empty();
        Map<String, Object> client = (Map<String, Object>) resourceAccess.get(CLIENT_ID);
        if (client == null) return Stream.empty();
        List<String> roles = (List<String>) client.getOrDefault(ROLES, List.of());
        return roles.stream().map(this::toAuthority);
    }

    /** Keycloak roles come in as "ROLE_AGENT" — use as-is for Spring. */
    private GrantedAuthority toAuthority(String role) {
        return new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    }
}
