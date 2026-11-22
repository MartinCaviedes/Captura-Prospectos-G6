package io.github.grupo6.prospects.service;

import io.github.grupo6.prospects.controller.ProspectRequests.*;
import io.github.grupo6.prospects.model.Prospect;
import io.github.grupo6.prospects.repository.ProspectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProspectService {
    private final ProspectRepository repository;
    public ProspectService(ProspectRepository repository) { this.repository = repository; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getClaimAsString("userId")); }
    private boolean role(Jwt jwt, String role) { return jwt.getClaimAsStringList("roles").contains(role); }
    private boolean site(Jwt jwt, String site) { return jwt.getClaimAsStringList("siteIds").contains(site); }
    private void require(boolean allowed) {
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
    private boolean supervisor(Jwt jwt, Prospect p) { return role(jwt,"SUPERVISOR") && site(jwt,p.getSiteId()); }
    private void access(Jwt jwt, Prospect p) {
        require(role(jwt,"ADMIN") || supervisor(jwt,p) || (role(jwt,"CAPTADOR") && actor(jwt).equals(p.getCaptorId())));
    }
    private Prospect find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NOT_FOUND"));
    }
    private void active(Prospect p) {
        if (p.getCancelledAt() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "PROSPECT_CANCELLED");
    }
    public Prospect create(Jwt jwt, Create input) {
        require(role(jwt,"CAPTADOR") && site(jwt,input.siteId()));
        var prospect = new Prospect(normalizeRut(input.rut()), input.eventId(), input.siteId(), actor(jwt),
            input.givenNames(), input.familyNames(), input.email(), input.phone(), input.consentVersion().trim());
        return repository.saveAndFlush(prospect);
    }
    @Transactional(readOnly = true)
    public Prospect get(Jwt jwt, UUID id) { var p = find(id); access(jwt,p); return p; }
    public record ProspectPage(List<Prospect> items, int page, int size, long total) {}
    @Transactional(readOnly = true)
    public ProspectPage list(Jwt jwt, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > 1000000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_PAGE");
        Specification<Prospect> scope = (root, query, cb) -> {
            if (role(jwt,"ADMIN")) return cb.conjunction();
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (role(jwt,"CAPTADOR")) predicates.add(cb.equal(root.get("captorId"),actor(jwt)));
            if (role(jwt,"SUPERVISOR")) predicates.add(root.get("siteId").in(jwt.getClaimAsStringList("siteIds")));
            return cb.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = repository.findAll(scope, PageRequest.of(page,size,Sort.by("createdAt","id")));
        return new ProspectPage(result.getContent(),page,size,result.getTotalElements());
    }
    public Prospect update(Jwt jwt, UUID id, Update input) {
        var p = find(id); access(jwt,p); active(p);
        p.update(input.givenNames(),input.familyNames(),input.email(),input.phone());
        return repository.saveAndFlush(p);
    }
    public Prospect cancel(Jwt jwt, UUID id, Cancel input) {
        var p = find(id); require(role(jwt,"ADMIN") || supervisor(jwt,p)); active(p);
        p.cancel(actor(jwt),input.reason()); return repository.saveAndFlush(p);
    }
    // Módulo 11, mismo formato de RUT del dominio local; no se comparte código Android.
    String normalizeRut(String raw) {
        String rut = raw.replace(".", "").trim().toUpperCase(java.util.Locale.ROOT);
        if (!rut.matches("[1-9][0-9]{6,7}-[0-9K]")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_RUT");
        String digits = rut.substring(0,rut.indexOf('-'));
        int sum = 0, weight = 2;
        for (int i = digits.length()-1; i >= 0; i--) { sum += (digits.charAt(i)-'0')*weight; weight = weight == 7 ? 2 : weight+1; }
        int check = 11 - sum % 11;
        char expected = check == 11 ? '0' : check == 10 ? 'K' : (char) ('0'+check);
        if (rut.charAt(rut.length()-1) != expected) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_RUT");
        return rut;
    }
}
