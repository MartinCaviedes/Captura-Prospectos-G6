package io.github.grupo6.users.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Historial administrativo sin nombres, correos, credenciales ni datos de prospectos. */
@Entity
@Table(name="account_access_audit")
public class AccountAccessAudit {
    @Id @Column(length=64) private String id;
    @Column(nullable=false,length=64) private String actorId;
    @Column(nullable=false,length=64) private String targetId;
    @Column(nullable=false,length=16) private String beforeStatus;
    @Column(nullable=false,length=16) private String afterStatus;
    @Column(nullable=false,length=32) private String beforeRole;
    @Column(nullable=false,length=32) private String afterRole;
    @Column(nullable=false,length=255) private String beforeSites;
    @Column(nullable=false,length=255) private String afterSites;
    @Column(nullable=false) private Instant changedAt;
    @Column(nullable=false) private long targetVersion;
    protected AccountAccessAudit() {}
    public AccountAccessAudit(String actor, Usuario user, String status, String role, String sites) {
        id=UUID.randomUUID().toString(); actorId=actor; targetId=user.getId();
        beforeStatus=status; afterStatus=user.getStatus().name();
        beforeRole=role; afterRole=user.getRol().name(); beforeSites=sites;
        afterSites=user.getSedesAsignadas(); changedAt=Instant.now(); targetVersion=user.getVersion();
    }
}
