package io.github.grupo6.prospects.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grupo6.prospects.controller.CaptureSyncController.*;
import io.github.grupo6.prospects.model.*;
import io.github.grupo6.prospects.repository.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CaptureSyncService {
    private final CaptureSyncRepository links;
    private final ProspectRepository prospects;
    private final ProspectService domain;
    private final ObjectMapper json;
    public CaptureSyncService(CaptureSyncRepository links,ProspectRepository prospects,ProspectService domain,ObjectMapper json) {
        this.links=links;this.prospects=prospects;this.domain=domain;this.json=json;
    }
    private void require(boolean ok,HttpStatus status,String code) {
        if(!ok) throw new ResponseStatusException(status,code);
    }
    @Transactional
    public Ack sync(Jwt jwt,UUID id,Request in) {
        var actor=UUID.fromString(jwt.getClaimAsString("userId"));
        var roles=jwt.getClaimAsStringList("roles");
        var r=in.registration();
        boolean supervisor=roles.contains("SUPERVISOR");
        require((roles.contains("CAPTADOR") || supervisor) && jwt.getClaimAsStringList("siteIds").contains(r.siteId()),HttpStatus.FORBIDDEN,"FORBIDDEN");
        String hash;
        try {
            var snapshot=json.readTree(in.snapshotJson());
            require(snapshot!=null && snapshot.isObject() && "F2027_1".equals(snapshot.path("schema").asText()),HttpStatus.BAD_REQUEST,"INVALID_SNAPSHOT");
            hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(in)));
        } catch(ResponseStatusException ex) { throw ex; }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"INVALID_SNAPSHOT"); }
        var link=links.locked(id).orElse(null);
        Prospect p;
        if(link==null) {
            require(actor.equals(in.ownerId()),HttpStatus.FORBIDDEN,"FORBIDDEN");
            require(in.expectedRevision()==0 && !"CANCELLED".equals(in.state()),HttpStatus.CONFLICT,"SYNC_BASE_MISSING");
            p=new Prospect(domain.normalizeRut(r.rut()),r.eventId(),r.siteId(),actor,r.givenNames(),r.familyNames(),r.email(),r.phone(),r.consentVersion().trim());
            p=prospects.saveAndFlush(p);
            link=new CaptureSync(id,p.getId(),in.deviceId());
        } else {
            p=prospects.findById(link.remoteId).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"SYNC_REMOTE_MISSING"));
            require(p.getCaptorId().equals(in.ownerId()) && (supervisor || actor.equals(p.getCaptorId())),HttpStatus.FORBIDDEN,"FORBIDDEN");
            require(link.deviceId.equals(in.deviceId()) && p.getRut().equals(domain.normalizeRut(r.rut())) && p.getEventId().equals(r.eventId()) && p.getSiteId().equals(r.siteId()) && p.getConsentVersion().equals(r.consentVersion().trim()),HttpStatus.CONFLICT,"SYNC_IDENTITY_CONFLICT");
            if(in.sourceRevision()==link.sourceRevision) {
                require(hash.equals(link.requestHash),HttpStatus.CONFLICT,"SYNC_REPLAY_CONFLICT");
                return new Ack(id,link.remoteId,link.sourceRevision,link.deviceId);
            }
            require(in.sourceRevision()>link.sourceRevision && in.expectedRevision()==link.sourceRevision && p.getVersion()==link.prospectVersion && p.getCancelledAt()==null,HttpStatus.CONFLICT,"SYNC_VERSION_CONFLICT");
            if(!actor.equals(p.getCaptorId())) require(in.reason()!=null && in.reason().trim().length()>=5,HttpStatus.BAD_REQUEST,"REASON_REQUIRED");
            p.update(r.givenNames(),r.familyNames(),r.email(),r.phone());
            if("CANCELLED".equals(in.state())) {
                require(in.reason()!=null && in.reason().trim().length()>=5,HttpStatus.BAD_REQUEST,"REASON_REQUIRED");
                p.cancel(actor,in.reason());
            }
            prospects.saveAndFlush(p);
        }
        link.sourceRevision=in.sourceRevision();link.prospectVersion=p.getVersion();link.requestHash=hash;link.snapshotJson=in.snapshotJson();
        links.saveAndFlush(link);
        return new Ack(id,p.getId(),in.sourceRevision(),in.deviceId());
    }
}
