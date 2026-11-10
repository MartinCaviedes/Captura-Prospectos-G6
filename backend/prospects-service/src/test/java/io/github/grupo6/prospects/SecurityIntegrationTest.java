package io.github.grupo6.prospects;

import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityIntegrationTest {
    static final KeyPair KEYS = keys();
    @Autowired MockMvc mvc;

    static KeyPair keys() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    static String pem(String type, byte[] bytes) throws Exception {
        var file = Files.createTempFile("jwt-test-", ".pem");
        file.toFile().deleteOnExit();
        Files.writeString(file, "-----BEGIN " + type + "-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
            + "\n-----END " + type + "-----\n");
        return file.toUri().toString();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var publicKey = pem("PUBLIC KEY", KEYS.getPublic().getEncoded());
        var privateKey = pem("PRIVATE KEY", KEYS.getPrivate().getEncoded());
        registry.add("security.jwt.public-key", () -> publicKey);
        registry.add("security.jwt.private-key", () -> privateKey);
        registry.add("LOCAL_ADMIN_PASSWORD", () -> "test-admin-password-only");
        registry.add("LOCAL_CAPTOR_PASSWORD", () -> "test-captor-password-only");
    }

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

    @Test void healthIsPublicButApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/prospects/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test void signedTokenWorksAndCaptorCannotAccessAdministration() throws Exception {
        String token = token(KEYS, "captura-g6-users", "captura-g6-api", "CAPTADOR", 900);
        mvc.perform(get("/api/v1/prospects/status")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.subject").value("test-user"));
        mvc.perform(get("/api/v1/admin/status").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/status").header("Authorization", "Bearer "
            + token(KEYS, "captura-g6-users", "captura-g6-api", "ADMIN", 900)))
            .andExpect(status().isOk());
    }

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
}
