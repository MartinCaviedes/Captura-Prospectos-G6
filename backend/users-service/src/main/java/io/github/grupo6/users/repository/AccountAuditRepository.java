package io.github.grupo6.users.repository;

public interface AccountAuditRepository extends org.springframework.data.jpa.repository.JpaRepository<
    io.github.grupo6.users.model.AccountAccessAudit,String> {
    long countByTargetId(String targetId);
}
