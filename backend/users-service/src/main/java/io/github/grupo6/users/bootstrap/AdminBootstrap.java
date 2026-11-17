package io.github.grupo6.users.bootstrap;

import io.github.grupo6.users.controller.StatusController.CrearUsuarioRequest;
import io.github.grupo6.users.model.RolUsuario;
import jakarta.validation.Validation;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Aprovisionamiento explícito, fuera del servidor HTTP, sobre el esquema PostgreSQL existente. */
public final class AdminBootstrap {
    private AdminBootstrap() {}

    public static int execute(Map<String, String> environment) {
        try {
            var request = new CrearUsuarioRequest(
                required(environment, "BOOTSTRAP_ADMIN_USERNAME").trim(),
                required(environment, "BOOTSTRAP_ADMIN_EMAIL").trim().toLowerCase(Locale.ROOT),
                required(environment, "BOOTSTRAP_ADMIN_DISPLAY_NAME").trim(),
                required(environment, "BOOTSTRAP_ADMIN_PASSWORD"), RolUsuario.ADMIN,
                environment.get("BOOTSTRAP_ADMIN_SITES"));
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                if (!factory.getValidator().validate(request).isEmpty()
                        || request.password().length() < 12
                        || request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
                    throw new IllegalArgumentException("Configuración de administrador inválida");
                }
            }
            String sites = request.sedesAsignadas();
            sites = sites == null || sites.isBlank() ? "site-1" : sites.trim();
            for (String site : sites.split(",", -1)) {
                if (site.isBlank() || site.trim().length() > 120) {
                    throw new IllegalArgumentException("Sedes inválidas");
                }
            }
            String url = required(environment, "IDENTITY_JDBC_URL");
            if (!url.startsWith("jdbc:postgresql:")) {
                throw new IllegalArgumentException("El bootstrap requiere PostgreSQL");
            }
            try (var connection = DriverManager.getConnection(url,
                    required(environment, "IDENTITY_DB_USER"), required(environment, "IDENTITY_DB_PASSWORD"))) {
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    statement.execute("SET LOCAL lock_timeout = '10s'");
                    statement.execute("LOCK TABLE usuarios IN EXCLUSIVE MODE");
                    try (var count = statement.executeQuery("SELECT count(*) FROM usuarios")) {
                        count.next();
                        if (count.getLong(1) != 0) {
                            throw new IllegalStateException("El bootstrap requiere una base sin cuentas");
                        }
                    }
                }
                try (var insert = connection.prepareStatement("INSERT INTO usuarios "
                        + "(id, username, email, display_name, password_hash, rol, sedes_asignadas, activo) "
                        + "VALUES (?, ?, ?, ?, ?, 'ADMIN', ?, true)")) {
                    insert.setString(1, UUID.randomUUID().toString());
                    insert.setString(2, request.username());
                    insert.setString(3, request.email());
                    insert.setString(4, request.displayName());
                    insert.setString(5, new BCryptPasswordEncoder().encode(request.password()));
                    insert.setString(6, sites);
                    insert.executeUpdate();
                }
                connection.commit();
            }
            System.out.println("Bootstrap de administrador completado");
            return 0;
        } catch (Exception error) {
            // Las excepciones JDBC pueden contener datos de conexión o valores SQL.
            System.err.println("Bootstrap rechazado. Revisar configuración, esquema, permisos y existencia de cuentas.");
            return 1;
        }
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta configuración requerida");
        }
        return value;
    }
}
