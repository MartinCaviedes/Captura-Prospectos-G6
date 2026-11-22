package io.github.grupo6.users.controller;

import io.github.grupo6.users.controller.AccountDtos.*;
import io.github.grupo6.users.model.*;
import io.github.grupo6.users.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class StatusController {
    private final UsuarioService users;
    private final SiteCatalog sites;
    public StatusController(UsuarioService users,SiteCatalog sites) { this.users=users; this.sites=sites; }
    public record CrearUsuarioRequest(@NotBlank @Size(max=160) String username,
        @NotBlank @Email @Size(max=160) String email,@NotBlank @Size(min=2,max=120) String displayName,
        @NotBlank @Size(min=6,max=128) String password,@NotNull RolUsuario rol,@Size(max=255) String sedesAsignadas) {
        @Override public String toString() { return "CrearUsuarioRequest[credentials=REDACTED]"; }
    }
    @GetMapping("/api/v1/me")
    public UserSummary identity(@AuthenticationPrincipal Jwt jwt) { return UserSummary.of(users.current(jwt)); }
    @GetMapping("/api/v1/admin/status")
    public Map<String,String> admin(@AuthenticationPrincipal Jwt jwt) { users.requireAdmin(jwt); return Map.of("service","users-service","status","ok"); }
    @GetMapping("/api/v1/admin/sites")
    public List<SiteCatalog.Site> catalog(@AuthenticationPrincipal Jwt jwt) { users.requireAdmin(jwt); return sites.all(); }
    @GetMapping("/api/v1/users")
    public UserPage list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) AccountStatus status,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return users.list(jwt,status,page,size); }
    @GetMapping("/api/v1/users/{id}")
    public UserSummary detail(@AuthenticationPrincipal Jwt jwt,@PathVariable String id) { return users.detail(jwt,id); }
    @PostMapping("/api/v1/users") @ResponseStatus(HttpStatus.CREATED)
    public UserSummary create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CrearUsuarioRequest input) {
        return users.createManaged(jwt,input.username(),input.email(),input.displayName(),input.password(),input.rol(),input.sedesAsignadas());
    }
    @PatchMapping("/api/v1/users/{id}/access")
    public UserSummary access(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,@Valid @RequestBody AccessRequest input) { return users.access(jwt,id,input); }
    @PatchMapping("/api/v1/users/{id}/estado")
    public UserSummary state(@AuthenticationPrincipal Jwt jwt,@PathVariable String id,@Valid @RequestBody StateRequest input) { return users.legacyState(jwt,id,input); }
}
