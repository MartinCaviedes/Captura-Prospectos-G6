package io.github.grupo6.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
                        @Value("${services.users.url}") String usersUrl,
                        @Value("${services.prospects.url}") String prospectsUrl) {
        return builder.routes()
            .route("users-auth", route -> route
                .path("/api/v1/auth/**")
                .filters(filters -> filters.removeRequestHeader("Cookie"))
                .uri(usersUrl))
            .route("users-me", route -> route
                .path("/api/v1/me")
                .filters(filters -> filters.removeRequestHeader("Cookie"))
                .uri(usersUrl))
            .route("prospects", route -> route
                .path(
                    "/api/v1/prospects/**",
                    "/api/v1/assignments/**",
                    "/api/v1/catalog/**",
                    "/api/v1/events/**",
                    "/api/v1/registrations/**",
                    "/api/v1/device-reports/**")
                .filters(filters -> filters.removeRequestHeader("Cookie"))
                .uri(prospectsUrl))
            .build();
    }
}
