package io.github.grupo6.gateway.security;

import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

/** Contrato emitido por usuarios de Jeremy; se valida antes de convertir autoridades. */
final class IdentityClaimsValidator implements OAuth2TokenValidator<Jwt> {
    private static final Set<String> ROLES = Set.of("CAPTADOR", "SUPERVISOR", "ADMIN");
    @Override public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var claims = jwt.getClaims();
        boolean valid = claims.get("userId") instanceof String id
            && id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
            && claims.get("sub") instanceof String subject && !subject.isBlank()
            && jwt.getExpiresAt() != null
            && claims.get("roles") instanceof List<?> roles && !roles.isEmpty()
            && roles.stream().allMatch(value -> value instanceof String role && ROLES.contains(role))
            && claims.get("siteIds") instanceof List<?> sites
            && sites.stream().allMatch(value -> value instanceof String site && !site.isBlank() && site.length() <= 120);
        return valid ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Incompatible identity claims", null));
    }
}
