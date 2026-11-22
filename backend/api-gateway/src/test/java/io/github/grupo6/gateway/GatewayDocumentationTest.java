package io.github.grupo6.gateway;

import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="documentation.enabled=true")
class GatewayDocumentationTest {
    @Autowired WebTestClient client;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) throws Exception {
        var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);
        var file=Files.createTempFile("g6-docs-public-", ".pem");file.toFile().deleteOnExit();
        var key=Base64.getMimeEncoder(64,new byte[]{'\n'}).encodeToString(generator.generateKeyPair().getPublic().getEncoded());
        Files.writeString(file,"-----BEGIN PUBLIC KEY-----\n"+key+"\n-----END PUBLIC KEY-----\n");
        registry.add("security.jwt.public-key",()->file.toUri().toString());
    }
    @Test void servesLocalUiAssetsAndCanonicalContract() {
        client.get().uri("/docs").exchange().expectStatus().isTemporaryRedirect()
            .expectHeader().valueEquals("Location","/docs/index.html");
        client.get().uri("/docs/index.html").exchange().expectStatus().isOk().expectBody(String.class)
            .value(html->{assertThat(html).contains("/docs/assets/swagger-ui-bundle.js");assertThat(html).doesNotContain("https://");});
        client.get().uri("/docs/assets/swagger-ui-bundle.js").exchange().expectStatus().isOk();
        client.get().uri("/docs/assets/swagger-ui.css").exchange().expectStatus().isOk();
        client.get().uri("/docs/openapi/backend.openapi.json").exchange().expectStatus().isOk().expectBody()
            .jsonPath("$.openapi").isEqualTo("3.1.0")
            .jsonPath("$.paths['/prospects/sync/{id}'].put.operationId").isEqualTo("syncCapture")
            .jsonPath("$.paths['/auth/register'].post.security").isEmpty()
            .jsonPath("$.servers[0].url").isEqualTo("/api/v1");
    }
    @Test void documentationDoesNotOpenApiRoutesOrOtherClasspathResources() {
        client.get().uri("/api/v1/me").exchange().expectStatus().isUnauthorized();
        client.get().uri("/api/v1/prospects").exchange().expectStatus().isUnauthorized();
        client.post().uri("/docs/index.html").exchange().expectStatus().isUnauthorized();
        client.get().uri("/docs/application.yml").exchange().expectStatus().isNotFound();
        client.get().uri("/docs/initializer.js").exchange().expectStatus().isOk().expectBody(String.class)
            .value(js->assertThat(js).contains("validatorUrl: null","persistAuthorization: false","queryConfigEnabled: false","window.location.origin"));
    }
}
