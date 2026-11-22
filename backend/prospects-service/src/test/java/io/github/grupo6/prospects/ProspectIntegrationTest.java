package io.github.grupo6.prospects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProspectIntegrationTest {
    static final String A = "11111111-1111-4111-8111-111111111111";
    static final String B = "22222222-2222-4222-8222-222222222222";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired io.github.grupo6.prospects.repository.CaptureSyncRepository syncLinks;
    @Autowired io.github.grupo6.prospects.repository.ProspectRepository prospects;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) throws Exception {
        SecurityIntegrationTest.properties(r);
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:prospect_tests;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    }
    String token(String user, String role, String site) {
        var keys = SecurityIntegrationTest.KEYS;
        var key = new RSAKey.Builder((RSAPublicKey) keys.getPublic()).privateKey((RSAPrivateKey) keys.getPrivate()).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),
            JwtClaimsSet.builder().issuer("captura-g6-users").audience(List.of("captura-g6-api"))
            .subject("username-" + user).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900))
            .claim("userId", user).claim("roles", List.of(role)).claim("siteIds", List.of(site)).build())).getTokenValue();
    }
    Map<String, Object> body(String event, String site) {
        return new HashMap<>(Map.of("rut", "12.345.678-5", "eventId", event, "siteId", site,
            "givenNames", "Ana", "familyNames", "Pérez", "email", "ana@example.com", "phone", "+56912345678",
            "consentAccepted", true, "consentVersion", "v1"));
    }
    String create(String actor, String role, String site, String event) throws Exception {
        var response = mvc.perform(post("/api/v1/prospects").header("Authorization", "Bearer " + token(actor, role, site))
            .contentType("application/json").content(json.writeValueAsString(body(event, site))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.captorId").value(actor))
            .andExpect(jsonPath("$.rut").value("12345678-5"))
            .andExpect(jsonPath("$.consentRecordedAt").isNotEmpty()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }
    Map<String,Object> syncBody(String actor,String event,long revision,long expected) {
        var request=new HashMap<String,Object>();
        request.put("deviceId","33333333-3333-4333-8333-333333333333");request.put("ownerId",actor);
        request.put("sourceRevision",revision);request.put("expectedRevision",expected);
        request.put("registration",body(event,"site-1"));request.put("state","NEEDS_CONTEXT");
        request.put("snapshotJson","{\"schema\":\"F2027_1\",\"interests\":[{\"priority\":1}],\"consent\":{\"version\":\"v1\"}}");
        return request;
    }
    String push(String id,String actor,String role,Map<String,Object> request,int expected) throws Exception {
        return mvc.perform(put("/api/v1/prospects/sync/"+id).header("Authorization","Bearer "+token(actor,role,"site-1"))
            .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().is(expected))
            .andReturn().getResponse().getContentAsString();
    }
    @Test void syncReplaysCreateUpdateAndCancellationWithoutDuplicates() throws Exception {
        String id=UUID.randomUUID().toString(),event=UUID.randomUUID().toString();
        var request=syncBody(A,event,3,0);String first=push(id,A,"CAPTADOR",request,200);
        assertEquals(first,push(id,A,"CAPTADOR",request,200));
        String remote=json.readTree(first).get("remoteId").asText();
        request.put("sourceRevision",4);request.put("expectedRevision",3);
        var registration=new HashMap<>((Map<String,Object>)request.get("registration"));registration.put("givenNames","Ana María");request.put("registration",registration);
        String updated=push(id,A,"CAPTADOR",request,200);assertEquals(updated,push(id,A,"CAPTADOR",request,200));
        request.put("sourceRevision",5);request.put("expectedRevision",4);request.put("state","CANCELLED");request.put("reason","Anulación de prueba");
        String cancelled=push(id,A,"CAPTADOR",request,200);assertEquals(cancelled,push(id,A,"CAPTADOR",request,200));
        mvc.perform(get("/api/v1/prospects/"+remote).header("Authorization","Bearer "+token(A,"CAPTADOR","site-1")))
            .andExpect(jsonPath("$.givenNames").value("Ana María")).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertTrue(syncLinks.findById(UUID.fromString(id)).orElseThrow().snapshotJson.contains("interests"));
    }
    @Test void syncRejectsChangedReplayAndOutOfOrderRevision() throws Exception {
        String id=UUID.randomUUID().toString();var request=syncBody(A,UUID.randomUUID().toString(),1,0);push(id,A,"CAPTADOR",request,200);
        request.put("snapshotJson","{\"schema\":\"F2027_1\",\"changed\":true}");
        assertEquals("SYNC_REPLAY_CONFLICT",json.readTree(push(id,A,"CAPTADOR",request,409)).get("code").asText());
        request.put("sourceRevision",2);request.put("expectedRevision",0);push(id,A,"CAPTADOR",request,409);
        request.put("expectedRevision",1);push(id,A,"CAPTADOR",request,200);
    }
    @Test void syncCannotImpersonateOwnerCrossSitesOrChangeDevice() throws Exception {
        String id=UUID.randomUUID().toString();var request=syncBody(A,UUID.randomUUID().toString(),1,0);
        push(id,B,"CAPTADOR",request,403);push(id,A,"ADMIN",request,403);
        var registration=(Map<String,Object>)request.get("registration");registration.put("siteId","site-2");push(id,A,"CAPTADOR",request,403);
        registration.put("siteId","site-1");push(id,A,"CAPTADOR",request,200);
        push(id,B,"CAPTADOR",request,403);
        request.put("deviceId",UUID.randomUUID().toString());request.put("sourceRevision",2);request.put("expectedRevision",1);push(id,A,"CAPTADOR",request,409);
    }
    @Test void syncDetectsExternalEditsInsteadOfOverwritingThem() throws Exception {
        String id=UUID.randomUUID().toString();var request=syncBody(A,UUID.randomUUID().toString(),1,0);
        String remote=json.readTree(push(id,A,"CAPTADOR",request,200)).get("remoteId").asText();
        mvc.perform(put("/api/v1/prospects/"+remote).header("Authorization","Bearer "+token(A,"CAPTADOR","site-1"))
            .contentType("application/json").content("{\"givenNames\":\"Cambio externo\",\"familyNames\":\"Prueba\",\"email\":\"externo@example.invalid\",\"phone\":\"+56912345678\"}"))
            .andExpect(status().isOk());
        request.put("sourceRevision",2);request.put("expectedRevision",1);push(id,A,"CAPTADOR",request,409);
    }
    @Test void syncValidatesPayloadAndKeepsBusinessDuplicateSeparateFromReplay() throws Exception {
        String id=UUID.randomUUID().toString();var request=syncBody(A,UUID.randomUUID().toString(),1,0);
        request.put("snapshotJson","not-json");push(id,A,"CAPTADOR",request,400);
        request.put("snapshotJson","{\"schema\":\"F2027_1\"}");push(id,A,"CAPTADOR",request,200);
        push(UUID.randomUUID().toString(),A,"CAPTADOR",request,409);
    }
    @Test void syncSupervisorCanCreateButOtherOwnerEditsRequireReason() throws Exception {
        String id=UUID.randomUUID().toString();var request=syncBody(A,UUID.randomUUID().toString(),1,0);push(id,A,"SUPERVISOR",request,200);
        request.put("sourceRevision",2);request.put("expectedRevision",1);push(id,B,"SUPERVISOR",request,400);
        request.put("reason","Corrección supervisada");push(id,B,"SUPERVISOR",request,200);
    }
    @Test void concurrentIdenticalSyncCreatesReturnTheSameAck() throws Exception {
        String id=UUID.randomUUID().toString(),event=UUID.randomUUID().toString();var request=syncBody(A,event,1,0);
        var barrier=new java.util.concurrent.CyclicBarrier(2);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var tasks=new ArrayList<java.util.concurrent.Future<String>>();
            for(int i=0;i<2;i++) tasks.add(pool.submit(()->{barrier.await(10,java.util.concurrent.TimeUnit.SECONDS);return push(id,A,"CAPTADOR",request,200);}));
            assertEquals(tasks.get(0).get(15,java.util.concurrent.TimeUnit.SECONDS),tasks.get(1).get(15,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(1L,prospects.findAll().stream().filter(p->p.getEventId().toString().equals(event)).count());
    }
    @Test void crudPersistsAndEnforcesOwnership() throws Exception {
        String id = create(A, "CAPTADOR", "site-1", UUID.randomUUID().toString());
        String owner = token(A, "CAPTADOR", "site-1"), other = token(B, "CAPTADOR", "site-1");
        mvc.perform(get("/api/v1/prospects/" + id).header("Authorization", "Bearer " + owner))
            .andExpect(status().isOk()).andExpect(jsonPath("$.givenNames").value("Ana"));
        mvc.perform(get("/api/v1/prospects/" + id).header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
        String update = "{\"givenNames\":\"Ana María\",\"familyNames\":\"Pérez\",\"email\":\"nuevo@example.com\",\"phone\":\"+56987654321\"}";
        mvc.perform(put("/api/v1/prospects/" + id).header("Authorization", "Bearer " + other)
            .contentType("application/json").content(update)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/prospects/" + id).header("Authorization", "Bearer " + owner)
            .contentType("application/json").content(update)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/prospects/" + id).header("Authorization", "Bearer " + owner))
            .andExpect(jsonPath("$.email").value("nuevo@example.com")).andExpect(jsonPath("$.captorId").value(A));
        var list = mvc.perform(get("/api/v1/prospects?size=100").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var item : json.readTree(list).get("items")) assertEquals(B, item.get("captorId").asText());
    }
    @Test void supervisorIsLimitedToAssignedSitesAndAdminCanReadAll() throws Exception {
        String id = create(A, "CAPTADOR", "site-2", UUID.randomUUID().toString());
        mvc.perform(get("/api/v1/prospects/" + id).header("Authorization", "Bearer " + token(B,"SUPERVISOR","site-1")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/prospects/" + id).header("Authorization", "Bearer " + token(B,"SUPERVISOR","site-2")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/prospects/" + id).header("Authorization", "Bearer " + token(B,"ADMIN","site-1")))
            .andExpect(status().isOk());
        var list = mvc.perform(get("/api/v1/prospects?size=100").header("Authorization", "Bearer " + token(B,"SUPERVISOR","site-1")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (var item : json.readTree(list).get("items")) assertEquals("site-1", item.get("siteId").asText());
    }
    @Test void duplicateCancellationAndRecreationPreserveAudit() throws Exception {
        String event = UUID.randomUUID().toString(), id = create(A,"CAPTADOR","site-1",event);
        mvc.perform(post("/api/v1/prospects").header("Authorization","Bearer " + token(B,"CAPTADOR","site-1"))
            .contentType("application/json").content(json.writeValueAsString(body(event,"site-1"))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_EVENT_REGISTRATION"));
        String cancel = "{\"reason\":\"Corrección supervisada\"}";
        for (String denied : List.of(token(A,"CAPTADOR","site-1"), token(B,"SUPERVISOR","site-2")))
            mvc.perform(post("/api/v1/prospects/"+id+"/cancel").header("Authorization","Bearer " + denied)
                .contentType("application/json").content(cancel)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/prospects/"+id+"/cancel").header("Authorization","Bearer " + token(B,"SUPERVISOR","site-1"))
            .contentType("application/json").content(cancel)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.cancelledBy").value(B))
            .andExpect(jsonPath("$.cancelledAt").isNotEmpty()).andExpect(jsonPath("$.cancellationReason").value("Corrección supervisada"));
        mvc.perform(post("/api/v1/prospects/"+id+"/cancel").header("Authorization","Bearer " + token(B,"ADMIN","site-1"))
            .contentType("application/json").content(cancel)).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/prospects/"+id).header("Authorization","Bearer " + token(A,"CAPTADOR","site-1"))
            .contentType("application/json").content("{\"givenNames\":\"Ana\",\"familyNames\":\"Pérez\",\"email\":\"ana@example.com\",\"phone\":\"+56912345678\"}"))
            .andExpect(status().isConflict());
        assertNotEquals(id, create(A,"CAPTADOR","site-1",event));
    }
    @Test void invalidInputMissingResourcesAndUntrustedFieldsAreRejected() throws Exception {
        String auth = "Bearer " + token(A,"CAPTADOR","site-1");
        var valid = body(UUID.randomUUID().toString(),"site-1");
        for (var mutation : List.of(Map.of("consentAccepted",false), Map.of("rut","12345678-9"),
                Map.of("givenNames"," "), Map.of("eventId","bad"), Map.of("email","bad"), Map.of("captorId",B))) {
            var invalid = new HashMap<>(valid); invalid.putAll(mutation);
            mvc.perform(post("/api/v1/prospects").header("Authorization",auth)
                .contentType("application/json").content(json.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        }
        mvc.perform(get("/api/v1/prospects/"+UUID.randomUUID()).header("Authorization",auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/prospects?size=101").header("Authorization",auth)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/prospects").header("Authorization",auth).contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/prospects").header("Authorization",auth).contentType("application/json")
            .content(json.writeValueAsString(body(UUID.randomUUID().toString(),"site-2")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/prospects")).andExpect(status().isUnauthorized());
    }
}
