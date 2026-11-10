package io.github.grupo6.users.controller;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas técnicas del esqueleto, no implementan CRUD ni autorización por propietario. */
@RestController
public class StatusController {
    @GetMapping("/api/v1/me")
    public Map<String, Object> identity(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("service", "users-service", "subject", jwt.getSubject(),
                      "roles", jwt.getClaimAsStringList("roles"));
    }

    @GetMapping("/api/v1/admin/status")
    public Map<String, String> admin() {
        return Map.of("service", "users-service", "status", "ok");
    }
}
