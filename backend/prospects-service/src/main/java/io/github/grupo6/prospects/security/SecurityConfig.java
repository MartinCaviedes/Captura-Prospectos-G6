package io.github.grupo6.prospects.security;

import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, com.fasterxml.jackson.databind.ObjectMapper json) throws Exception {
        return http
            // API Bearer sin cookies de sesión: CSRF no aplica a estas rutas.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                .authenticationEntryPoint((request,response,error) -> {
                    response.setStatus(401); response.setHeader("WWW-Authenticate", "Bearer");
                    response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
                    json.writeValue(response.getOutputStream(), io.github.grupo6.prospects.controller.ApiErrors.error("UNAUTHORIZED","Se requiere un JWT válido"));
                }))
            .exceptionHandling(errors -> errors.accessDeniedHandler((request,response,error) -> {
                response.setStatus(403); response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
                json.writeValue(response.getOutputStream(), io.github.grupo6.prospects.controller.ApiErrors.error("FORBIDDEN","No tiene permiso para esta operación"));
            }))
            .build();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${security.jwt.public-key}") RSAPublicKey publicKey,
                          @Value("${security.jwt.issuer}") String issuer,
                          @Value("${security.jwt.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withPublicKey(publicKey)
            .signatureAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, new IdentityClaimsValidator()));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
