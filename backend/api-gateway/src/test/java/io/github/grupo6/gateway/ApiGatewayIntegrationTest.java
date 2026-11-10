package io.github.grupo6.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiGatewayIntegrationTest {
    static final KeyPair KEYS = keys();
    static final HttpServer USERS = server("users");
    static final HttpServer PROSPECTS = server("prospects");

    @Autowired
    WebTestClient client;

    static KeyPair keys() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static HttpServer server(String service) {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> respond(exchange, service));
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static void respond(HttpExchange exchange, String service) throws IOException {
        boolean authorizationPresent = exchange.getRequestHeaders().containsKey("Authorization");
        String response = "{\"service\":\"" + service
            + "\",\"authorizationPresent\":" + authorizationPresent + "}";
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    static String publicKeyResource() throws IOException {
        var file = Files.createTempFile("gateway-jwt-test-", ".pem");
        file.toFile().deleteOnExit();
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
            .encodeToString(KEYS.getPublic().getEncoded());
        Files.writeString(file, "-----BEGIN PUBLIC KEY-----\n" + encoded
            + "\n-----END PUBLIC KEY-----\n");
        return file.toUri().toString();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        String publicKey = publicKeyResource();
        registry.add("security.jwt.public-key", () -> publicKey);
        registry.add("services.users.url",
            () -> "http://127.0.0.1:" + USERS.getAddress().getPort());
        registry.add("services.prospects.url",
            () -> "http://127.0.0.1:" + PROSPECTS.getAddress().getPort());
    }

    String token(KeyPair keys, String issuer, String audience, long expiresIn) {
        var rsa = new RSAKey.Builder((RSAPublicKey) keys.getPublic())
            .privateKey((RSAPrivateKey) keys.getPrivate()).build();
        var encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsa)));
        var claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject("test-user")
            .audience(List.of(audience))
            .issuedAt(Instant.now().minusSeconds(120))
            .expiresAt(Instant.now().plusSeconds(expiresIn))
            .claim("roles", List.of("CAPTADOR"))
            .build();
        return encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }

    @Test
    void healthAndLoginArePublicAndLoginRoutesToUsers() {
        client.get().uri("/actuator/health").exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.status").isEqualTo("UP");

        client.post().uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"username\":\"demo\",\"password\":\"demo\"}")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.service").isEqualTo("users")
            .jsonPath("$.authorizationPresent").isEqualTo(false);
    }

    @Test
    void protectedRoutesRequireJwtAndForwardItToTheCorrectService() {
        client.get().uri("/api/v1/prospects/status").exchange()
            .expectStatus().isUnauthorized();

        String token = token(KEYS, "captura-g6-users", "captura-g6-api", 900);
        for (String path : List.of(
            "/api/v1/prospects/status",
            "/api/v1/assignments/current",
            "/api/v1/catalog/current",
            "/api/v1/events/current",
            "/api/v1/registrations/current",
            "/api/v1/device-reports/current")) {
            client.get().uri(path)
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.service").isEqualTo("prospects")
                .jsonPath("$.authorizationPresent").isEqualTo(true);
        }

        client.get().uri("/api/v1/me")
            .headers(headers -> headers.setBearerAuth(token))
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.service").isEqualTo("users")
            .jsonPath("$.authorizationPresent").isEqualTo(true);
    }

    @Test
    void invalidIssuerAudienceExpirationAndSignatureAreRejected() {
        for (String token : List.of(
            token(KEYS, "other", "captura-g6-api", 900),
            token(KEYS, "captura-g6-users", "other", 900),
            token(KEYS, "captura-g6-users", "captura-g6-api", -120),
            token(keys(), "captura-g6-users", "captura-g6-api", 900))) {
            client.get().uri("/api/v1/prospects/status")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isUnauthorized();
        }
    }

    @AfterAll
    static void stopServers() {
        USERS.stop(0);
        PROSPECTS.stop(0);
    }
}
