package io.github.grupo6.prospects.controller;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class ProspectRequests {
    private ProspectRequests() {}
    public record Create(
        @NotBlank @Size(max = 20) String rut,
        @NotNull UUID eventId,
        @NotBlank @Size(max = 120) String siteId,
        @NotBlank @Size(max = 120) String givenNames,
        @NotBlank @Size(max = 120) String familyNames,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{8,15}") String phone,
        @NotNull @AssertTrue Boolean consentAccepted,
        @NotBlank @Size(max = 80) String consentVersion) {}
    public record Update(
        @NotBlank @Size(max = 120) String givenNames,
        @NotBlank @Size(max = 120) String familyNames,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Pattern(regexp = "\\+?[0-9]{8,15}") String phone) {}
    public record Cancel(@NotBlank @Size(max = 500) String reason) {}
}
