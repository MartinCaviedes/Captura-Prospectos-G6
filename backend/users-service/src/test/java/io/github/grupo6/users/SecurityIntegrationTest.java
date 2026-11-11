package io.github.grupo6.users;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración para login persistido, validación JWT y perfiles de usuario (~75% cobertura).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityIntegrationTest {
    static final KeyPair KEYS = keys();
    @Autowired MockMvc mvc;

    /**
     * Genera un par de llaves RSA de 2048 bits para firmar tokens en pruebas.
     */
    static KeyPair keys() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /**
     * Escribe una llave criptográfica temporal en formato PEM para configurar el contexto.
     */
    static String pem(String type, byte[] bytes) throws Exception {
        var file = Files.createTempFile("jwt-test-", ".pem");
        file.toFile().deleteOnExit();
        Files.writeString(file, "-----BEGIN " + type + "-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
            + "\n-----END " + type + "-----\n");
        return file.toUri().toString();
    }

    /**
     * Registra dinámicamente las llaves RSA y contraseñas semilla para el entorno de prueba.
     */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var publicKey = pem("PUBLIC KEY", KEYS.getPublic().getEncoded());
        var privateKey = pem("PRIVATE KEY", KEYS.getPrivate().getEncoded());
        registry.add("security.jwt.public-key", () -> publicKey);
        registry.add("security.jwt.private-key", () -> privateKey);
        registry.add("LOCAL_ADMIN_PASSWORD", () -> "test-admin-password-only");
        registry.add("LOCAL_CAPTOR_PASSWORD", () -> "test-captor-password-only");
    }

    /**
     * Construye un token JWT firmado con los parámetros indicados para pruebas de autorización.
     */
    String token(KeyPair keys, String issuer, String audience, String role, long expiry) {
        var rsa = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
            .privateKey((RSAPrivateKey) keys.getPrivate()).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
        var claims = JwtClaimsSet.builder().issuer(issuer).subject("test-user")
            .audience(List.of(audience)).issuedAt(Instant.now().minusSeconds(1000))
            .expiresAt(Instant.now().plusSeconds(expiry)).claim("roles", List.of(role)).build();
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    /**
     * Comprueba que el endpoint de salud sea público y el perfil requiera token.
     */
    @Test void healthIsPublicButApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
    }

    /**
     * Verifica control de acceso por roles en rutas protegidas de captador y administrador.
     */
    @Test void signedTokenWorksAndCaptorCannotAccessAdministration() throws Exception {
        String token = token(KEYS, "captura-g6-users", "captura-g6-api", "CAPTADOR", 900);
        mvc.perform(get("/api/v1/me")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subject").value("test-user"));
        mvc.perform(get("/api/v1/admin/status").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        String adminToken = token(KEYS, "captura-g6-users", "captura-g6-api", "ADMIN", 900);
        mvc.perform(get("/api/v1/admin/status").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
    }

    /**
     * Verifica que tokens con firma, audiencia, emisor o expiración inválida sean rechazados.
     */
    @Test void invalidIssuerAudienceExpirationAndSignatureAreRejected() throws Exception {
        for (String token : List.of(
            token(KEYS, "other", "captura-g6-api", "ADMIN", 900),
            token(KEYS, "captura-g6-users", "other", "ADMIN", 900),
            token(KEYS, "captura-g6-users", "captura-g6-api", "ADMIN", -120),
            token(keys(), "captura-g6-users", "captura-g6-api", "ADMIN", 900))) {
            mvc.perform(get("/api/v1/admin/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    /**
     * Valida login contra base de datos por username o correo de la app Android.
     */
    @Test void loginValidatesCredentialsAndReturnsUsableBearerToken() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
            .content("{\"username\":\"demo-captor\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
            .content("{\"username\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest());
        var response = mvc.perform(post("/api/v1/auth/login").contentType("application/json")
            .content("{\"username\":\"captador.a@g6.example\",\"password\":\"test-captor-password-only\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.email").value("captador.a@g6.example"))
            .andExpect(jsonPath("$.rol").value("CAPTADOR"))
            .andReturn().getResponse().getContentAsString();
        String accessToken = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response).get("accessToken").asText();
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").value("captor-a"))
            .andExpect(jsonPath("$.email").value("captador.a@g6.example"));
    }
}
