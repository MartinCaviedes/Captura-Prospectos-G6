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
    public static final long SESSION_SECONDS = 4 * 60 * 60;

    private final UsuarioService usuarioService;
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    /**
     * Inicializa el controlador de autenticación con el gestor de seguridad y emisor JWT.
     */
    public AuthController(UsuarioService usuarioService,
                          JwtEncoder encoder,
                          @Value("${security.jwt.issuer}") String issuer,
                          @Value("${security.jwt.audience}") String audience) {
        this.usuarioService = usuarioService;
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    public record LoginRequest(@NotBlank @Size(max = 160) String username,
                               @NotBlank @Size(max = 128) String password) {
        @Override public String toString() { return "LoginRequest[credentials=REDACTED]"; }
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AccountDtos.RegistrationReceipt register(@Valid @RequestBody AccountDtos.RegisterRequest request) {
        return usuarioService.register(request);
    }

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
        Usuario usuario = usuarioService.authenticate(request.username(), request.password());
        var now = Instant.now();
        var roles = List.of(usuario.getRol().name());
        String userId = usuario.getId();
        String email = usuario.getEmail();
        String displayName = usuario.getDisplayName();
        String rolPrincipal = usuario.getRol().name();
        List<String> siteIds = usuario.obtenerSedeIds();

        var claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(usuario.getUsername())
            .audience(List.of(audience))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(SESSION_SECONDS))
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
            SESSION_SECONDS,
            userId,
            usuario.getUsername(),
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
        return Map.of("code", "INVALID_REQUEST", "message", "Revisa los datos enviados.");
    }
}
