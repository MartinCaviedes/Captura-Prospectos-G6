package io.github.grupo6.users;

import com.fasterxml.jackson.databind.*;
import io.github.grupo6.users.model.*;
import io.github.grupo6.users.repository.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("local")
class AccountAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UsuarioRepository users;
    @Autowired AccountAuditRepository audit;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        SecurityIntegrationTest.properties(registry);
        registry.add("spring.datasource.url",() -> "jdbc:h2:mem:account_access;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
    }
    String bearer(String role) {
        return "Bearer " + new SecurityIntegrationTest().token(SecurityIntegrationTest.KEYS,"captura-g6-users","captura-g6-api",role,900);
    }
    record Reply(int status,JsonNode body) {}
    Reply call(String method,String path,Object body,String auth) throws Exception {
        var req=MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),path);
        if (auth!=null) req.header("Authorization",auth);
        if (body!=null) req.contentType("application/json").content(mapper.writeValueAsBytes(body));
        var response=mvc.perform(req).andReturn().getResponse();
        return new Reply(response.getStatus(),response.getContentAsByteArray().length==0 ? mapper.nullNode() : mapper.readTree(response.getContentAsByteArray()));
    }
    Map<String,Object> registration() {
        return new HashMap<>(Map.of("displayName","Persona ficticia","email",UUID.randomUUID()+"@example.invalid","password","ClaveDemo-2026"));
    }
    String pending(Map<String,Object> payload) throws Exception {
        Reply result=call("POST","/api/v1/auth/register",payload,null);
        assertEquals(201,result.status(),result.body().toString());
        assertEquals("PENDING",result.body().path("status").asText());
        assertFalse(result.body().has("accessToken"));
        return result.body().path("userId").asText();
    }
    Map<String,Object> access(long version,String role,List<String> sites,String status) {
        var result=new HashMap<String,Object>();
        result.put("expectedVersion",version); result.put("role",role); result.put("siteIds",sites); result.put("status",status);
        return result;
    }
    Reply login(Map<String,Object> payload,String password) throws Exception {
        return call("POST","/api/v1/auth/login",Map.of("username",payload.get("email"),"password",password),null);
    }
    void error(Reply reply,int status,String code) {
        assertEquals(status,reply.status(),reply.body().toString()); assertEquals(code,reply.body().path("code").asText());
    }

    @Test void pendingRegistrationChecksPasswordBeforeRevealingStatusAndReturnsNoSecrets() throws Exception {
        var payload=registration(); String id=pending(payload);
        error(login(payload,"Incorrecta"),401,"INVALID_CREDENTIALS");
        error(login(payload,(String)payload.get("password")),403,"ACCOUNT_PENDING");
        var detail=call("GET","/api/v1/users/"+id,null,bearer("ADMIN"));
        assertEquals(200,detail.status()); assertTrue(detail.body().path("role").isNull());
        assertEquals(0,detail.body().path("siteIds").size());
        assertFalse(detail.body().has("passwordHash")); assertFalse(detail.body().has("password"));
        var stored=users.findById(id).orElseThrow(); assertFalse(stored.isActivo());
        assertNotEquals(payload.get("password"),stored.getPasswordHash()); assertTrue(stored.getPasswordHash().startsWith("$2"));
    }

    @Test void publicRegistrationRejectsPrivilegeInjectionInvalidFieldsAndBcryptByteOverflow() throws Exception {
        for (String field:List.of("role","rol","siteIds","status","activo")) {
            var payload=registration(); payload.put(field,field.equals("siteIds") ? List.of("site-1") : "ADMIN");
            error(call("POST","/api/v1/auth/register",payload,null),400,"INVALID_REQUEST");
            assertFalse(users.existsByEmailIgnoreCase((String)payload.get("email")));
        }
        var payload=registration(); payload.put("password","á".repeat(37));
        error(call("POST","/api/v1/auth/register",payload,null),400,"INVALID_REQUEST");
        payload.put("password","corta"); error(call("POST","/api/v1/auth/register",payload,null),400,"INVALID_REQUEST");
        payload=registration(); payload.put("email","invalido");
        error(call("POST","/api/v1/auth/register",payload,null),400,"INVALID_REQUEST");
    }

    @Test void approvalSupportsLoginAndVersionedNoopButRejectsStaleEdits() throws Exception {
        var payload=registration(); String id=pending(payload), path="/api/v1/users/"+id+"/access";
        var approved=call("PATCH",path,access(0,"SUPERVISOR",List.of("site-2","site-1"),"ACTIVE"),bearer("ADMIN"));
        assertEquals(200,approved.status(),approved.body().toString());
        assertEquals(1,approved.body().path("version").asLong());
        assertEquals("SUPERVISOR",login(payload,(String)payload.get("password")).body().path("rol").asText());
        var noop=call("PATCH",path,access(1,"SUPERVISOR",List.of("site-1","site-2"),"ACTIVE"),bearer("ADMIN"));
        assertEquals(200,noop.status()); assertEquals(1,noop.body().path("version").asLong());
        error(call("PATCH",path,access(0,"CAPTADOR",List.of("site-1"),"ACTIVE"),bearer("ADMIN")),409,"VERSION_CONFLICT");
        assertEquals(1,audit.countByTargetId(id));
    }

    @Test void invalidAssignmentsDoNotActivateOrAdvanceVersion() throws Exception {
        String id=pending(registration()), path="/api/v1/users/"+id+"/access";
        for (var input:List.of(access(0,"ADMIN",List.of("site-1"),"ACTIVE"),access(0,null,List.of("site-1"),"ACTIVE"),
            access(0,"CAPTADOR",List.of(),"ACTIVE"),access(0,"CAPTADOR",List.of("site-no-existe"),"ACTIVE"),
            access(0,"CAPTADOR",List.of("site-1","site-1"),"ACTIVE"),access(0,"CAPTADOR",List.of("site-1"),"PENDING")))
            error(call("PATCH",path,input,bearer("ADMIN")),400,"INVALID_REQUEST");
        assertEquals(AccountStatus.PENDING,users.findById(id).orElseThrow().getStatus());
        assertEquals(0,users.findById(id).orElseThrow().getVersion());
        assertEquals(0,audit.countByTargetId(id));
    }

    @Test void captorAndSupervisorCannotManageUsersEvenWithDirectHttpCalls() throws Exception {
        String id=pending(registration());
        for (String role:List.of("CAPTADOR","SUPERVISOR")) {
            for (String path:List.of("/api/v1/users","/api/v1/users/"+id,"/api/v1/admin/sites"))
                error(call("GET",path,null,bearer(role)),403,"ACCESS_DENIED");
            error(call("PATCH","/api/v1/users/"+id+"/access",access(0,"CAPTADOR",List.of("site-1"),"ACTIVE"),bearer(role)),403,"ACCESS_DENIED");
        }
        assertEquals(401,call("GET","/api/v1/users",null,null).status());
    }

    @Test void disablePendingRequiresNoAssignmentsAndLegacyRouteCannotApproveIt() throws Exception {
        String id=pending(registration()), path="/api/v1/users/"+id;
        error(call("PATCH",path+"/estado",Map.of("activo",true,"expectedVersion",0),bearer("ADMIN")),409,"VERSION_CONFLICT");
        error(call("PATCH",path+"/estado",Map.of("activo",false),bearer("ADMIN")),400,"INVALID_REQUEST");
        var result=call("PATCH",path+"/access",access(0,null,List.of(),"DISABLED"),bearer("ADMIN"));
        assertEquals(200,result.status()); assertTrue(result.body().path("role").isNull());
        error(call("PATCH",path+"/estado",Map.of("activo",true,"expectedVersion",1),bearer("ADMIN")),409,"VERSION_CONFLICT");
    }

    @Test void disablingBlocksLoginAndMeReportsCurrentStateWithExistingToken() throws Exception {
        var payload=registration(); String id=pending(payload), path="/api/v1/users/"+id;
        call("PATCH",path+"/access",access(0,"CAPTADOR",List.of("site-1"),"ACTIVE"),bearer("ADMIN"));
        String existing="Bearer "+login(payload,(String)payload.get("password")).body().path("accessToken").asText();
        var disabled=call("PATCH",path+"/estado",Map.of("activo",false,"expectedVersion",1),bearer("ADMIN"));
        assertEquals(200,disabled.status());
        error(login(payload,"Incorrecta"),401,"INVALID_CREDENTIALS");
        error(login(payload,(String)payload.get("password")),403,"ACCOUNT_DISABLED");
        var me=call("GET","/api/v1/me",null,existing);
        assertEquals(200,me.status()); assertEquals("DISABLED",me.body().path("status").asText());
    }

    @Test void administratorsAreProtectedThroughBothMutationRoutes() throws Exception {
        String id=users.findByUsernameIgnoreCase("demo-admin").orElseThrow().getId();
        error(call("PATCH","/api/v1/users/"+id+"/access",access(0,"CAPTADOR",List.of("site-1"),"DISABLED"),bearer("ADMIN")),409,"ADMIN_PROTECTED");
        error(call("PATCH","/api/v1/users/"+id+"/estado",Map.of("activo",false,"expectedVersion",0),bearer("ADMIN")),409,"ADMIN_PROTECTED");
        assertTrue(users.findById(id).orElseThrow().isActivo());
    }

    @Test void twoConcurrentApprovalsProduceOneCommitAndOneConflict() throws Exception {
        String id=pending(registration()), path="/api/v1/users/"+id+"/access";
        String admin=bearer("ADMIN"); var gate=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<Reply> task=() -> { gate.await(); return call("PATCH",path,access(0,"CAPTADOR",List.of("site-1"),"ACTIVE"),admin); };
            var a=pool.submit(task); var b=pool.submit(task); gate.countDown();
            var codes=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS).status(),b.get(15,TimeUnit.SECONDS).status()));
            Collections.sort(codes); assertEquals(List.of(200,409),codes);
        }
        assertEquals(1,users.findById(id).orElseThrow().getVersion());
        assertEquals(1,audit.countByTargetId(id));
    }

    @Test void concurrentDuplicateRegistrationCannotCreateTwoAccounts() throws Exception {
        var payload=registration(); var upper=new HashMap<>(payload); upper.put("email",payload.get("email").toString().toUpperCase(Locale.ROOT));
        var gate=new CountDownLatch(1);
        try (var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(() -> {gate.await(); return call("POST","/api/v1/auth/register",payload,null);});
            var b=pool.submit(() -> {gate.await(); return call("POST","/api/v1/auth/register",upper,null);}); gate.countDown();
            var codes=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS).status(),b.get(15,TimeUnit.SECONDS).status()));
            Collections.sort(codes); assertEquals(List.of(201,409),codes);
        }
    }

    @Test void catalogPaginationAndManagedCreationHaveExplicitContracts() throws Exception {
        var catalog=call("GET","/api/v1/admin/sites",null,bearer("ADMIN"));
        assertEquals(200,catalog.status()); assertEquals(8,catalog.body().size());
        String id=pending(registration());
        var page=call("GET","/api/v1/users?status=PENDING&page=0&size=1",null,bearer("ADMIN"));
        assertEquals(200,page.status()); assertEquals(1,page.body().path("items").size());
        error(call("GET","/api/v1/users?size=101",null,bearer("ADMIN")),400,"INVALID_REQUEST");
        error(call("GET","/api/v1/users/"+UUID.randomUUID(),null,bearer("ADMIN")),404,"USER_NOT_FOUND");
        var input=registration(); input.put("username",UUID.randomUUID().toString()); input.put("rol","CAPTADOR"); input.put("sedesAsignadas","site-1");
        assertEquals(201,call("POST","/api/v1/users",input,bearer("ADMIN")).status());
        input.put("username",UUID.randomUUID().toString()); input.put("email",UUID.randomUUID()+"@example.invalid"); input.put("rol","ADMIN");
        error(call("POST","/api/v1/users",input,bearer("ADMIN")),400,"INVALID_REQUEST");
    }

    @Test void fullLengthEmailCanRegisterAndLoginAfterApproval() throws Exception {
        var input=registration(); input.put("email",UUID.randomUUID()+"@"+"a".repeat(63)+"."+"b".repeat(51)+".invalid");
        String id=pending(input);
        assertEquals(200,call("PATCH","/api/v1/users/"+id+"/access",access(0,"CAPTADOR",List.of("site-1"),"ACTIVE"),bearer("ADMIN")).status());
        assertEquals(200,login(input,(String)input.get("password")).status());
    }
}
