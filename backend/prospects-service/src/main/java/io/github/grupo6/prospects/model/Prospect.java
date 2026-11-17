package io.github.grupo6.prospects.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prospects", uniqueConstraints = @UniqueConstraint(name = "uk_prospect_active", columnNames = "active_key"))
public class Prospect {
    @Id private UUID id;
    @Version private long version;
    @Column(nullable = false, length = 12) private String rut;
    @Column(nullable = false) private UUID eventId;
    @Column(nullable = false, length = 120) private String siteId;
    @Column(nullable = false) private UUID captorId;
    @Column(nullable = false, length = 120) private String givenNames;
    @Column(nullable = false, length = 120) private String familyNames;
    @Column(nullable = false, length = 160) private String email;
    @Column(nullable = false, length = 20) private String phone;
    @Column(nullable = false, length = 80) private String consentVersion;
    @Column(nullable = false) private Instant consentRecordedAt;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant cancelledAt;
    private UUID cancelledBy;
    @Column(length = 500) private String cancellationReason;
    @Column(name = "active_key", length = 64) private String activeKey;

    protected Prospect() {}

    public Prospect(String rut, UUID eventId, String siteId, UUID captorId,
                    String givenNames, String familyNames, String email, String phone, String consentVersion) {
        this.id = UUID.randomUUID(); this.rut = rut; this.eventId = eventId;
        this.siteId = siteId; this.captorId = captorId; this.consentVersion = consentVersion;
        this.createdAt = Instant.now(); this.consentRecordedAt = createdAt;
        this.activeKey = rut + ":" + eventId;
        update(givenNames, familyNames, email, phone);
    }
    public void update(String givenNames, String familyNames, String email, String phone) {
        this.givenNames = givenNames.trim(); this.familyNames = familyNames.trim();
        this.email = email.trim(); this.phone = phone.trim(); this.updatedAt = Instant.now();
    }
    public void cancel(UUID actor, String reason) {
        this.cancelledBy = actor; this.cancellationReason = reason.trim();
        this.cancelledAt = Instant.now(); this.updatedAt = cancelledAt; this.activeKey = null;
    }
    public String getStatus() { return cancelledAt == null ? "ACTIVE" : "CANCELLED"; }
    public UUID getId() { return id; }
    public String getRut() { return rut; }
    public UUID getEventId() { return eventId; }
    public String getSiteId() { return siteId; }
    public UUID getCaptorId() { return captorId; }
    public String getGivenNames() { return givenNames; }
    public String getFamilyNames() { return familyNames; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getConsentVersion() { return consentVersion; }
    public Instant getConsentRecordedAt() { return consentRecordedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public UUID getCancelledBy() { return cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
}
