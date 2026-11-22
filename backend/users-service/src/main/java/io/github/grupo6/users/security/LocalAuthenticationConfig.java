package io.github.grupo6.users.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Configuración de proveedores criptográficos BCrypt, autenticación persistida en BD y firma JWT.
 */
@Configuration
public class LocalAuthenticationConfig {

    /**
     * Provee el codificador BCrypt para cifrar y verificar contraseñas de usuarios.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura el gestor de autenticación conectado al servicio de usuarios persistidos en BD.
     */
    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    /**
     * Configura el codificador Nimbus RSA para firmar los tokens JWT emitidos en el login.
     */
    @Bean
    JwtEncoder jwtEncoder(@Value("${security.jwt.public-key}") RSAPublicKey publicKey,
                          @Value("${security.jwt.private-key}") RSAPrivateKey privateKey) {
        var key = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("local-key").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
