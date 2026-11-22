package io.github.grupo6.prospects.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="capture_sync")
public class CaptureSync {
    @Id public UUID localCaptureId;
    @Version public Long version;
    @Column(nullable=false, unique=true) public UUID remoteId;
    @Column(nullable=false) public UUID deviceId;
    @Column(nullable=false) public long sourceRevision;
    @Column(nullable=false) public long prospectVersion;
    @Column(nullable=false,length=64) public String requestHash;
    @Column(nullable=false,columnDefinition="text") public String snapshotJson;
    protected CaptureSync() {}
    public CaptureSync(UUID localCaptureId, UUID remoteId, UUID deviceId) {
        this.localCaptureId=localCaptureId;this.remoteId=remoteId;this.deviceId=deviceId;
    }
}
