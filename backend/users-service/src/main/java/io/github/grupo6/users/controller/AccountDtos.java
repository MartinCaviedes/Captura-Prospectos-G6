package io.github.grupo6.users.controller;

import io.github.grupo6.users.model.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class AccountDtos {
    private AccountDtos() {}
    public record RegisterRequest(@NotBlank @Size(min=2,max=120) String displayName,
        @NotBlank @Email @Size(max=160) String email,
        @NotBlank @Size(min=6,max=128) String password) {
        @Override public String toString() { return "RegisterRequest[credentials=REDACTED]"; }
    }
    public record RegistrationReceipt(String userId, AccountStatus status, String message) {}
    public record UserSummary(String id, String username, String email, String displayName,
        AccountStatus status, RolUsuario role, List<String> siteIds, long version,
        Instant createdAt, Instant updatedAt) {
        public static UserSummary of(Usuario user) {
            var role = user.getStatus() == AccountStatus.PENDING || user.getSedesAsignadas().isBlank()
                && user.getRol() != RolUsuario.ADMIN ? null : user.getRol();
            return new UserSummary(user.getId(),user.getUsername(),user.getEmail(),user.getDisplayName(),
                user.getStatus(),role,role == null ? List.of() : user.obtenerSedeIds(),user.getVersion(),
                user.getCreatedAt(),user.getUpdatedAt());
        }
    }
    public record UserPage(List<UserSummary> items, int page, int size, long total) {}
    public record AccessRequest(@NotNull AccountStatus status, RolUsuario role,
        @NotNull @Size(max=8) List<@NotBlank @Size(max=120) String> siteIds,
        @NotNull @Min(0) Long expectedVersion) {}
    public record StateRequest(@NotNull Boolean activo, @NotNull @Min(0) Long expectedVersion) {}
}
