package io.github.grupo6.prospects.controller;

import io.github.grupo6.prospects.service.CaptureSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/prospects/sync")
public class CaptureSyncController {
    public record Request(@NotNull UUID deviceId, @NotNull UUID ownerId,
        @Min(1) long sourceRevision, @Min(0) long expectedRevision,
        @NotNull @Valid ProspectRequests.Create registration,
        @NotNull @Pattern(regexp="READY|NEEDS_CONTEXT|CANCELLED") String state,
        @Size(max=300) String reason, @NotBlank @Size(max=64000) String snapshotJson) {}
    public record Ack(UUID localCaptureId, UUID remoteId, long sourceRevision, UUID deviceId) {}
    private final CaptureSyncService service;
    public CaptureSyncController(CaptureSyncService service) { this.service=service; }
    @PutMapping("/{id}")
    public Ack sync(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody Request input) {
        try { return service.sync(jwt,id,input); }
        catch(org.springframework.dao.DataIntegrityViolationException race) {
            // Una creación simultánea puede ganar antes de que exista el enlace bloqueable.
            // La transacción fallida ya terminó: comprobar replay una sola vez en otra nueva.
            return service.sync(jwt,id,input);
        }
    }
}
