package io.github.grupo6.users.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** Autenticación de desarrollo; sustituir por cuentas persistidas al implementar usuarios. */
@Configuration
@Profile("local")
public class LocalAuthenticationConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(PasswordEncoder encoder,
            @Value("${LOCAL_ADMIN_PASSWORD}") String adminPassword,
            @Value("${LOCAL_CAPTOR_PASSWORD}") String captorPassword) {
        if (adminPassword.isBlank() || captorPassword.isBlank()) {
            throw new IllegalArgumentException("Configurar las contraseñas locales antes de iniciar");
        }
        var users = new InMemoryUserDetailsManager(
            User.withUsername("demo-admin").password(encoder.encode(adminPassword)).roles("ADMIN").build(),
            User.withUsername("demo-captor").password(encoder.encode(captorPassword)).roles("CAPTADOR").build());
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${security.jwt.public-key}") RSAPublicKey publicKey,
                          @Value("${security.jwt.private-key}") RSAPrivateKey privateKey) {
        var key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("local-key").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
