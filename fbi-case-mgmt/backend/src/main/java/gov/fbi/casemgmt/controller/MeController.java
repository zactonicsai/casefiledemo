package gov.fbi.casemgmt.controller;

import gov.fbi.casemgmt.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Identity")
public class MeController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return the current user's identity")
    public Me me() {
        return new Me(
            SecurityUtils.currentUsername().orElse("unknown"),
            SecurityUtils.currentRoles(),
            SecurityUtils.currentJwt().map(j -> j.getClaimAsString("email")).orElse(null),
            SecurityUtils.currentJwt().map(j -> j.getClaimAsString("name")).orElse(null),
            SecurityUtils.currentBadgeNumber().orElse(null)
        );
    }

    public record Me(String username, Collection<String> roles,
                     String email, String fullName, String badgeNumber) {}
}
