package io.github.grupo6.users.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import io.github.grupo6.users.controller.AccountException;

/** Catálogo versionado de las ocho sedes del incremento F2027; no acepta IDs por su forma. */
@Component
public class SiteCatalog {
    public record Site(String id, String name, boolean enabled) {}
    private final List<Site> sites;
    public SiteCatalog(ObjectMapper mapper) throws java.io.IOException {
        try (var input = new ClassPathResource("catalog/sites.json").getInputStream()) {
            sites = List.of(mapper.readValue(input, Site[].class));
        }
    }
    public List<Site> all() { return sites; }
    public List<String> validate(List<String> values) {
        if (values == null || values.size() > 8 || values.stream().distinct().count() != values.size()
            || values.stream().anyMatch(id -> sites.stream().noneMatch(s -> s.enabled() && s.id().equals(id))))
            throw new AccountException(400,"INVALID_REQUEST","Selecciona sedes válidas sin duplicados.");
        return values.stream().sorted().toList();
    }
}
