package io.github.grupo6.users.service;

import io.github.grupo6.users.model.RolUsuario;
import io.github.grupo6.users.model.Usuario;
import io.github.grupo6.users.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de cuentas base sincronizado con los perfiles de la aplicación Android.
 */
@Component
public class UsuarioDataSeeder implements CommandLineRunner {

    private static final String NAMESPACE_PREFIX = "g6-synthetic-context-v1/";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String captorPassword;

    /**
     * Construye el sembrador de datos con las contraseñas configuradas del entorno.
     */
    public UsuarioDataSeeder(UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${LOCAL_ADMIN_PASSWORD:admin1234}") String adminPassword,
                             @Value("${LOCAL_CAPTOR_PASSWORD:captor1234}") String captorPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.captorPassword = captorPassword;
    }

    /**
     * Ejecuta la carga inicial de usuarios demo y perfiles Android al iniciar el microservicio.
     */
    @Override
    public void run(String... args) {
        if (adminPassword.isBlank() || captorPassword.isBlank()) {
            throw new IllegalArgumentException("Configurar las contraseñas locales antes de iniciar");
        }
        sembrarCuenta("profile/demo-admin", "demo-admin", "admin@g6.example",
            "Administrador General", adminPassword, RolUsuario.ADMIN, "site-1,site-2");
        sembrarCuenta("profile/demo-captor", "demo-captor", "demo.captor@g6.example",
            "Captador Demo", captorPassword, RolUsuario.CAPTADOR, "site-1");
        sembrarCuenta("profile/captor-a", "captor-a", "captador.a@g6.example",
            "Captador A (demo)", captorPassword, RolUsuario.CAPTADOR, "site-1");
        sembrarCuenta("profile/captor-b", "captor-b", "captador.b@g6.example",
            "Captador B (demo)", captorPassword, RolUsuario.CAPTADOR, "site-2");
        sembrarCuenta("profile/supervisor", "supervisor", "supervisor@g6.example",
            "Supervisor (demo)", adminPassword, RolUsuario.SUPERVISOR, "site-1,site-2");
    }

    /**
     * Crea o actualiza una cuenta semilla garantizando su disponibilidad para login.
     */
    private void sembrarCuenta(String seedKey, String username, String email,
                               String displayName, String rawPassword,
                               RolUsuario rol, String sedesAsignadas) {
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        String id = generarUuidDeterministico(seedKey);
        Usuario usuario = new Usuario(
            id,
            username,
            email,
            displayName,
            passwordEncoder.encode(rawPassword),
            rol,
            sedesAsignadas,
            true
        );
        usuarioRepository.save(usuario);
    }

    /**
     * Genera un UUID determinístico idéntico al empleado por DemoSeed en la app Kotlin.
     */
    private String generarUuidDeterministico(String key) {
        byte[] bytes = (NAMESPACE_PREFIX + key).getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(bytes).toString();
    }
}
