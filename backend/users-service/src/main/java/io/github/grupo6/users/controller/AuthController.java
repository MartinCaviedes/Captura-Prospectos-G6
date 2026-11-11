package io.github.grupo6.users.controller;

import io.github.grupo6.users.model.Usuario;
import io.github.grupo6.users.service.UsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el inicio y cierre de sesión de usuarios con emisión de JWT.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    /**
     * Inicializa el controlador de autenticación con el gestor de seguridad y emisor JWT.
     */
    public AuthController(AuthenticationManager authenticationManager,
                          UsuarioService usuarioService,
                          JwtEncoder encoder,
                          @Value("${security.jwt.issuer}") String issuer,
                          @Value("${security.jwt.audience}") String audience) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    public record LoginRequest(@NotBlank @Size(max = 120) String username,
                               @NotBlank @Size(max = 128) String password) {}

    public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String userId,
        String username,
        String email,
        String displayName,
        String rol,
        List<String> roles,
        List<String> siteIds
    ) {}

    /**
     * Valida las credenciales contra la base de datos y retorna un token JWT firmado.
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username().trim(), request.password()));
        var now = Instant.now();
        var roles = auth.getAuthorities().stream()
            .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", "")).toList();

        Usuario usuario = usuarioService.buscarPorIdentificador(auth.getName()).orElse(null);
        String userId = usuario != null ? usuario.getId() : auth.getName();
        String email = usuario != null ? usuario.getEmail() : auth.getName();
        String displayName = usuario != null ? usuario.getDisplayName() : auth.getName();
        String rolPrincipal = usuario != null ? usuario.getRol().name() : (roles.isEmpty() ? "CAPTADOR" : roles.get(0));
        List<String> siteIds = usuario != null ? usuario.obtenerSedeIds() : List.of("site-1");

        var claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(auth.getName())
            .audience(List.of(audience))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .claim("roles", roles)
            .claim("userId", userId)
            .claim("email", email)
            .claim("displayName", displayName)
            .claim("siteIds", siteIds)
            .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenResponse(
            token,
            "Bearer",
            900,
            userId,
            auth.getName(),
            email,
            displayName,
            rolPrincipal,
            roles,
            siteIds
        );
    }

    /**
     * Confirma el cierre de sesión para que el cliente móvil limpie su contexto local.
     */
    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("status", "LOGGED_OUT", "message", "Sesión cerrada correctamente");
    }

    /**
     * Responde con estado HTTP 401 cuando el usuario o la contraseña son incorrectos.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> invalidCredentials() {
        return Map.of("code", "INVALID_CREDENTIALS", "message", "Credenciales inválidas");
    }

    /**
     * Responde con estado HTTP 400 cuando la petición de login incumple validaciones de formato.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest() {
        return Map.of("code", "INVALID_REQUEST", "message", "Revisar usuario y contraseña");
    }
}
