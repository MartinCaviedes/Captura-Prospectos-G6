package io.github.grupo6.users;

import io.github.grupo6.users.bootstrap.AdminBootstrap;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Rechazos anteriores a JDBC: ninguna prueba conecta a una base o crea cuentas. */
class AdminBootstrapValidationTest {
    private Map<String,String> valid() {
        return new HashMap<>(Map.of(
            "BOOTSTRAP_ADMIN_USERNAME", "admin-ficticio",
            "BOOTSTRAP_ADMIN_EMAIL", "ADMIN@example.invalid",
            "BOOTSTRAP_ADMIN_DISPLAY_NAME", "Administración ficticia",
            "BOOTSTRAP_ADMIN_PASSWORD", "Clave-ficticia-2026",
            "IDENTITY_JDBC_URL", "jdbc:h2:mem:not-for-bootstrap"));
    }

    @Test void missingOrBlankConfigurationIsRejected() {
        assertEquals(1,AdminBootstrap.execute(Map.of()));
        var env=valid();env.put("BOOTSTRAP_ADMIN_USERNAME"," ");
        assertEquals(1,AdminBootstrap.execute(env));
    }

    @Test void invalidIdentityAndUnsafePasswordAreRejectedBeforeDatabaseAccess() {
        for (var entry:Map.of("BOOTSTRAP_ADMIN_EMAIL","correo-invalido",
                "BOOTSTRAP_ADMIN_PASSWORD","corta").entrySet()) {
            var env=valid();env.put(entry.getKey(),entry.getValue());
            assertEquals(1,AdminBootstrap.execute(env));
        }
        var env=valid();env.put("BOOTSTRAP_ADMIN_PASSWORD","á".repeat(37));
        assertEquals(1,AdminBootstrap.execute(env));
    }

    @Test void emptySiteEntriesAndOversizedSitesAreRejected() {
        for (String sites:new String[]{"site-1,,site-2","site-1, ","x".repeat(121)}) {
            var env=valid();env.put("BOOTSTRAP_ADMIN_SITES",sites);
            assertEquals(1,AdminBootstrap.execute(env));
        }
    }

    @Test void nonPostgresDatabaseIsRejectedWithDefaultOrExplicitSites() {
        for (String sites:new String[]{"","site-1,site-2"}) {
            var env=valid();env.put("BOOTSTRAP_ADMIN_SITES",sites);
            assertEquals(1,AdminBootstrap.execute(env));
        }
        var env=valid();env.remove("IDENTITY_JDBC_URL");
        assertEquals(1,AdminBootstrap.execute(env));
    }
}
