package io.github.grupo6.users;

import io.github.grupo6.users.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:seed_isolation",
    "spring.datasource.username=sa",
    "spring.datasource.password=test-only",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles({"prod", "local"})
class SeedIsolationTest {
    @DynamicPropertySource
    static void keys(DynamicPropertyRegistry registry) throws Exception {
        SecurityIntegrationTest.properties(registry);
    }

    @Test
    void productionNeverCreatesDemoAccounts(@Autowired UsuarioRepository users) {
        assertEquals(0, users.count());
    }
}
