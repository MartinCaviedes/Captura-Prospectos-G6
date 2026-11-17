package io.github.grupo6.prospects.controller;

import io.github.grupo6.prospects.model.Prospect;
import io.github.grupo6.prospects.service.ProspectService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prospects")
public class ProspectController {
    private final ProspectService service;
    public ProspectController(ProspectService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Prospect create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProspectRequests.Create input) {
        return service.create(jwt,input);
    }
    @GetMapping("/{id}")
    public Prospect get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return service.get(jwt,id); }
    @GetMapping
    public ProspectService.ProspectPage list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return service.list(jwt,page,size);
    }
    @PutMapping("/{id}")
    public Prospect update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProspectRequests.Update input) {
        return service.update(jwt,id,input);
    }
    @PostMapping("/{id}/cancel")
    public Prospect cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProspectRequests.Cancel input) {
        return service.cancel(jwt,id,input);
    }
}
