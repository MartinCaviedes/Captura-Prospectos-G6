package io.github.grupo6.gateway.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.*;

/** Sirve el contrato versionado y Swagger UI del propio JAR, sin CDN ni escaneo de rutas futuras. */
@Configuration
@ConditionalOnProperty(name="documentation.enabled", havingValue="true")
public class GatewayDocumentation implements WebFluxConfigurer {
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/docs/assets/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.35.1/");
        registry.addResourceHandler("/docs/**").addResourceLocations("classpath:/api-documentation/");
    }
    @Bean RouterFunction<ServerResponse> documentationRoot() {
        return RouterFunctions.route(RequestPredicates.GET("/docs"),
            request -> ServerResponse.temporaryRedirect(URI.create("/docs/index.html")).build());
    }
}
