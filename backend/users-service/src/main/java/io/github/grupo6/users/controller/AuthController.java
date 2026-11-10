package io.github.grupo6.users.controller;

import java.time.Instant;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

/** Login mínimo de desarrollo. No implementa registro, refresh ni administración de cuentas. */
@RestController
@Profile("local")
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;

    public AuthController(AuthenticationManager authenticationManager, JwtEncoder encoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience) {
        this.authenticationManager = authenticationManager;
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    public record LoginRequest(@NotBlank @Size(max = 120) String username,
                               @NotBlank @Size(max = 128) String password) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var auth = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        var now = Instant.now();
        var roles = auth.getAuthorities().stream()
            .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", "")).toList();
        var claims = JwtClaimsSet.builder().issuer(issuer).subject(auth.getName())
            .audience(java.util.List.of(audience)).issuedAt(now).expiresAt(now.plusSeconds(900))
            .claim("roles", roles).build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", 900);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> invalidCredentials() {
        return Map.of("code", "INVALID_CREDENTIALS", "message", "Credenciales inválidas");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest() {
        return Map.of("code", "INVALID_REQUEST", "message", "Revisar usuario y contraseña");
    }
}
