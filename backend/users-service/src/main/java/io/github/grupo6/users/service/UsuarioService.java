package io.github.grupo6.users.service;

import io.github.grupo6.users.controller.AccountDtos.*;
import io.github.grupo6.users.controller.AccountException;
import io.github.grupo6.users.model.*;
import io.github.grupo6.users.repository.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService implements UserDetailsService {
    private final UsuarioRepository users;
    private final PasswordEncoder passwords;
    private final SiteCatalog sites;
    private final AccountAuditRepository audit;
    private final String dummyHash;
    public UsuarioService(UsuarioRepository users, PasswordEncoder passwords, SiteCatalog sites, AccountAuditRepository audit) {
        this.users=users; this.passwords=passwords; this.sites=sites; this.audit=audit;
        dummyHash=passwords.encode("unused-comparison-value");
    }
    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String identifier) {
        var user=buscarPorIdentificador(identifier).orElseThrow(() -> new UsernameNotFoundException("Cuenta no encontrada"));
        return User.withUsername(user.getUsername()).password(user.getPasswordHash())
            .disabled(user.getStatus()!=AccountStatus.ACTIVE).roles(user.getRol().name()).build();
    }
    @Transactional(readOnly=true)
    public Optional<Usuario> buscarPorIdentificador(String identifier) {
        String value=identifier==null ? "" : identifier.trim();
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(value,value);
    }
    /** Validar contraseña antes de revelar estado, sin prechecks de DaoAuthenticationProvider. */
    @Transactional(readOnly=true)
    public Usuario authenticate(String identifier,String password) {
        var user=buscarPorIdentificador(identifier).orElse(null);
        boolean matches=password.getBytes(StandardCharsets.UTF_8).length<=72
            && passwords.matches(password,user==null ? dummyHash : user.getPasswordHash());
        if (!matches || user==null) throw new BadCredentialsException("Credenciales inválidas");
        if (user.getStatus()!=AccountStatus.ACTIVE)
            throw new AccountException(403,"ACCOUNT_"+user.getStatus(),user.getStatus()==AccountStatus.PENDING
                ? "Tu cuenta espera habilitación administrativa." : "Tu cuenta está desactivada.");
        return user;
    }
    @Transactional(readOnly=true)
    public Usuario current(Jwt jwt) {
        return buscarPorIdentificador(jwt.getSubject()).filter(u -> u.getId().equals(jwt.getClaimAsString("userId")))
            .orElseThrow(() -> new AccountException(401,"INVALID_TOKEN","La cuenta de la sesión no está disponible."));
    }
    @Transactional(readOnly=true)
    public Usuario requireAdmin(Jwt jwt) {
        Usuario actor=current(jwt);
        if (actor.getStatus()!=AccountStatus.ACTIVE || actor.getRol()!=RolUsuario.ADMIN
            || jwt.getClaimAsStringList("roles")==null || !jwt.getClaimAsStringList("roles").contains("ADMIN"))
            throw new AccountException(403,"ACCESS_DENIED","No tienes permiso para administrar usuarios.");
        return actor;
    }
    @Transactional
    public RegistrationReceipt register(RegisterRequest request) {
        String email=normalize(request.email());
        Usuario user=create(email,email,request.displayName(),request.password(),RolUsuario.CAPTADOR,List.of(),AccountStatus.PENDING);
        return new RegistrationReceipt(user.getId(),AccountStatus.PENDING,"Solicitud recibida. El administrador debe habilitar tu cuenta.");
    }
    @Transactional
    public UserSummary createManaged(Jwt jwt,String username,String email,String name,String password,RolUsuario role,String assignedSites) {
        Usuario actor=requireAdmin(jwt);
        if (username.contains("@")) throw invalid("El nombre de usuario administrativo no debe contener @; utiliza el correo en el campo email.");
        if (role==null || role==RolUsuario.ADMIN) throw invalid("El rol debe ser Captador o Supervisor.");
        var assigned=sites.validate(assignedSites==null ? List.of() : Arrays.stream(assignedSites.split(",",-1)).map(String::trim).toList());
        if (assigned.isEmpty()) throw invalid("Selecciona al menos una sede.");
        Usuario user=create(username,email,name,password,role,assigned,AccountStatus.ACTIVE);
        audit.save(new AccountAccessAudit(actor.getId(),user,"NEW","NONE",""));
        return UserSummary.of(user);
    }
    private Usuario create(String username,String email,String name,String password,RolUsuario role,List<String> assigned,AccountStatus status) {
        if (password.getBytes(StandardCharsets.UTF_8).length>72) throw invalid("La contraseña supera 72 bytes UTF-8.");
        String login=normalize(username), mail=normalize(email), display=name.trim();
        if (display.codePointCount(0,display.length())<2 || login.isBlank() || mail.isBlank()) throw invalid("Revisa los datos de la cuenta.");
        if (buscarPorIdentificador(login).isPresent() || buscarPorIdentificador(mail).isPresent())
            throw new AccountException(409,"ACCOUNT_EXISTS","La cuenta ya existe.");
        var user=new Usuario(UUID.randomUUID().toString(),login,mail,display,passwords.encode(password),role,String.join(",",assigned),status==AccountStatus.ACTIVE);
        user.setStatus(status);
        return users.saveAndFlush(user);
    }
    @Transactional(readOnly=true)
    public UserPage list(Jwt jwt,AccountStatus status,int page,int size) {
        requireAdmin(jwt);
        if (page<0 || page>1000000 || size<1 || size>100) throw invalid("Paginación inválida.");
        var paging=PageRequest.of(page,size,Sort.by("createdAt","id"));
        var result=status==null ? users.findAll(paging) : users.findByStatus(status,paging);
        return new UserPage(result.getContent().stream().map(UserSummary::of).toList(),page,size,result.getTotalElements());
    }
    @Transactional(readOnly=true)
    public UserSummary detail(Jwt jwt,String id) { requireAdmin(jwt); return UserSummary.of(find(id)); }
    @Transactional
    public UserSummary access(Jwt jwt,String id,AccessRequest request) {
        Usuario actor=requireAdmin(jwt);
        Usuario target=users.lockById(id).orElseThrow(UsuarioService::missing);
        return change(actor,target,request);
    }
    @Transactional
    public UserSummary legacyState(Jwt jwt,String id,StateRequest request) {
        Usuario actor=requireAdmin(jwt);
        Usuario target=users.lockById(id).orElseThrow(UsuarioService::missing);
        if (request.activo() && (target.getStatus()==AccountStatus.PENDING || target.obtenerSedeIds().isEmpty()))
            throw new AccountException(409,"VERSION_CONFLICT","Usa la gestión de permisos para habilitar esta cuenta.");
        RolUsuario role=target.getStatus()==AccountStatus.PENDING || target.getSedesAsignadas().isBlank() ? null : target.getRol();
        return change(actor,target,new AccessRequest(request.activo() ? AccountStatus.ACTIVE : AccountStatus.DISABLED,role,target.obtenerSedeIds(),request.expectedVersion()));
    }
    private UserSummary change(Usuario actor,Usuario target,AccessRequest request) {
        // Proteger todos los ADMIN impide desactivar la propia cuenta o el último administrador, incluso en paralelo.
        if (target.getRol()==RolUsuario.ADMIN) throw new AccountException(409,"ADMIN_PROTECTED","La cuenta administradora está protegida.");
        if (request.expectedVersion()==null || request.expectedVersion()!=target.getVersion())
            throw new AccountException(409,"VERSION_CONFLICT","Actualiza los datos antes de guardar.");
        if (request.status()==null || request.status()==AccountStatus.PENDING || request.role()==RolUsuario.ADMIN) throw invalid("Estado o rol no permitido.");
        List<String> assigned=sites.validate(request.siteIds());
        if (request.status()==AccountStatus.ACTIVE && (request.role()==null || assigned.isEmpty())) throw invalid("Una cuenta activa requiere rol y sedes.");
        if (request.role()==null && (!assigned.isEmpty() || !target.getSedesAsignadas().isBlank())) throw invalid("Solo solicitudes sin asignaciones pueden conservar rol vacío al desactivarse.");
        RolUsuario role=request.role()==null ? RolUsuario.CAPTADOR : request.role();
        String joined=String.join(",",assigned);
        if (target.getStatus()==request.status() && target.getRol()==role && target.getSedesAsignadas().equals(joined)) return UserSummary.of(target);
        String oldStatus=target.getStatus().name(), oldRole=target.getRol().name(), oldSites=target.getSedesAsignadas();
        target.setStatus(request.status()); target.setRol(role); target.setSedesAsignadas(joined); target.touch();
        users.flush(); // Actualizar @Version antes de construir respuesta/auditoría.
        audit.saveAndFlush(new AccountAccessAudit(actor.getId(),target,oldStatus,oldRole,oldSites));
        return UserSummary.of(target);
    }
    private Usuario find(String id) { return users.findById(id).orElseThrow(UsuarioService::missing); }
    private static AccountException missing() { return new AccountException(404,"USER_NOT_FOUND","Cuenta no encontrada."); }
    private static AccountException invalid(String message) { return new AccountException(400,"INVALID_REQUEST",message); }
    private static String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
}
